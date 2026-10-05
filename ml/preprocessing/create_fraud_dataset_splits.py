from collections import Counter
from pathlib import Path

import pandas as pd


RAW_DATASET = Path("ml/dataset/paysim.csv")
OUTPUT_DIRECTORY = Path("ml/dataset")
CHUNK_SIZE = 250_000

FEATURE_COLUMNS = ["amount", "accountBalance", "transactionType", "isFraud"]
SOURCE_COLUMNS = ["step", "type", "amount", "oldbalanceOrg", "isFraud"]
TRANSACTION_TYPES = ["PAYMENT", "TRANSFER", "CASH_OUT", "DEBIT", "CASH_IN"]

TRAIN_OUTPUT = OUTPUT_DIRECTORY / "fraud_train.csv"
VALIDATION_OUTPUT = OUTPUT_DIRECTORY / "fraud_validation.csv"
TEST_OUTPUT = OUTPUT_DIRECTORY / "fraud_test.csv"


def validate_chunk(chunk: pd.DataFrame) -> None:
    if chunk[SOURCE_COLUMNS].isna().any().any():
        raise ValueError("PaySim contains a missing value in a required column.")

    if (chunk["amount"] < 0).any():
        raise ValueError("PaySim contains a negative transaction amount.")

    if (chunk["oldbalanceOrg"] < 0).any():
        raise ValueError("PaySim contains a negative origin balance.")

    if not chunk["isFraud"].isin([0, 1]).all():
        raise ValueError("PaySim contains an isFraud value other than 0 or 1.")

    unexpected_types = set(chunk["type"].unique()) - set(TRANSACTION_TYPES)
    if unexpected_types:
        raise ValueError(f"PaySim contains unsupported transaction types: {unexpected_types}")


def determine_step_ranges() -> tuple[int, int, int, int, int, int]:
    steps: set[int] = set()

    for chunk in pd.read_csv(
            RAW_DATASET,
            usecols=SOURCE_COLUMNS,
            chunksize=CHUNK_SIZE):
        validate_chunk(chunk)
        steps.update(chunk["step"].astype(int).unique())

    sorted_steps = sorted(steps)
    if len(sorted_steps) < 3:
        raise ValueError("PaySim does not contain enough chronological steps to split.")

    train_step_count = int(len(sorted_steps) * 0.70)
    validation_step_count = int(len(sorted_steps) * 0.15)

    if train_step_count == 0 or validation_step_count == 0:
        raise ValueError("Chronological split would create an empty partition.")

    train_last_step = sorted_steps[train_step_count - 1]
    validation_last_step = sorted_steps[
        train_step_count + validation_step_count - 1
    ]

    return (
        sorted_steps[0],
        train_last_step,
        train_last_step + 1,
        validation_last_step,
        validation_last_step + 1,
        sorted_steps[-1],
    )


def write_split_datasets(
        train_last_step: int,
        validation_last_step: int) -> dict[str, Counter]:
    outputs = {
        "train": TRAIN_OUTPUT,
        "validation": VALIDATION_OUTPUT,
        "test": TEST_OUTPUT,
    }
    temporary_outputs = {
        name: output.with_suffix(output.suffix + ".tmp")
        for name, output in outputs.items()
    }
    statistics = {name: Counter() for name in outputs}

    for temporary_output in temporary_outputs.values():
        temporary_output.unlink(missing_ok=True)

    try:
        for chunk in pd.read_csv(
                RAW_DATASET,
                usecols=SOURCE_COLUMNS,
                chunksize=CHUNK_SIZE):
            validate_chunk(chunk)

            prepared = chunk.rename(columns={
                "oldbalanceOrg": "accountBalance",
                "type": "transactionType",
            })[FEATURE_COLUMNS]

            partition_masks = {
                "train": chunk["step"] <= train_last_step,
                "validation": (
                    (chunk["step"] > train_last_step)
                    & (chunk["step"] <= validation_last_step)
                ),
                "test": chunk["step"] > validation_last_step,
            }

            for name, mask in partition_masks.items():
                partition = prepared.loc[mask]
                if partition.empty:
                    continue

                partition.to_csv(
                    temporary_outputs[name],
                    mode="a",
                    header=not temporary_outputs[name].exists(),
                    index=False,
                )
                statistics[name]["rows"] += len(partition)
                statistics[name]["fraud"] += int(partition["isFraud"].sum())

        for name, temporary_output in temporary_outputs.items():
            if not temporary_output.exists():
                raise ValueError(f"The {name} split is empty.")
            temporary_output.replace(outputs[name])

        return statistics
    except Exception:
        for temporary_output in temporary_outputs.values():
            temporary_output.unlink(missing_ok=True)
        raise


def print_statistics(
        ranges: tuple[int, int, int, int, int, int],
        statistics: dict[str, Counter]) -> None:
    train_start, train_end, validation_start, validation_end, test_start, test_end = ranges
    print("Created chronological PaySim datasets")
    print(f"PaySim steps: {train_start}-{test_end}")
    print(f"Training steps: {train_start}-{train_end}")
    print(f"Validation steps: {validation_start}-{validation_end}")
    print(f"Test steps: {test_start}-{test_end}")

    for name in ("train", "validation", "test"):
        rows = statistics[name]["rows"]
        fraud = statistics[name]["fraud"]
        non_fraud = rows - fraud
        fraud_percentage = 100 * fraud / rows
        print(
            f"{name}: rows={rows}, fraud={fraud}, "
            f"nonFraud={non_fraud}, fraudPercentage={fraud_percentage:.6f}"
        )


def main() -> None:
    if not RAW_DATASET.exists():
        raise FileNotFoundError(f"Raw PaySim dataset not found: {RAW_DATASET}")

    ranges = determine_step_ranges()
    statistics = write_split_datasets(ranges[1], ranges[3])
    print_statistics(ranges, statistics)


if __name__ == "__main__":
    main()
