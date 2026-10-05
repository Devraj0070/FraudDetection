import pandas as pd

INPUT_FILE = "ml/dataset/paysim_training.csv"

TRAIN_FILE = "ml/dataset/paysim_train.csv"
TEST_FILE = "ml/dataset/paysim_test.csv"

print("Loading dataset...")

df = pd.read_csv(INPUT_FILE)

# Make isFraud the last column
feature_columns = [
    column
    for column in df.columns
    if column != "isFraud"
]

df = df[
    feature_columns + ["isFraud"]
]

# Shuffle the dataset
df = df.sample(
    frac=1,
    random_state=42
).reset_index(drop=True)

# 80% training
split_index = int(len(df) * 0.8)

train_df = df[:split_index]
test_df = df[split_index:]

train_df.to_csv(
    TRAIN_FILE,
    index=False
)

test_df.to_csv(
    TEST_FILE,
    index=False
)

print("Dataset split successfully.")
print("Training rows:", len(train_df))
print("Testing rows:", len(test_df))
print("isFraud is last:", train_df.columns[-1] == "isFraud")