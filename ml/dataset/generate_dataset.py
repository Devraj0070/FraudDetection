import csv
import random

random.seed(42)

OUTPUT_FILE = "ml/dataset/fraud_transactions_generated.csv"

ROWS = 2000

with open(OUTPUT_FILE, "w", newline="") as file:

    writer = csv.writer(file)

    writer.writerow([
        "amount",
        "transactionHour",
        "transactionDayOfWeek",
        "recentTransactionCount",
        "averageTransactionAmount",
        "amountDifferenceFromAverage",
        "fraud"
    ])

    for i in range(ROWS):

        # User's normal transaction amount
        average_amount = random.uniform(500, 5000)

        # Normal transaction amount
        amount = random.gauss(
            average_amount,
            average_amount * 0.20
        )

        amount = max(50, amount)

        transaction_hour = random.randint(0, 23)

        transaction_day = random.randint(0, 6)

        recent_count = random.randint(0, 12)

        difference = abs(amount - average_amount)

        # Calculate how unusual the transaction is.
        deviation_ratio = difference / average_amount

        fraud_probability = 0.03

        # Large deviation from normal behaviour.
        if deviation_ratio > 0.50:
            fraud_probability += 0.25

        if deviation_ratio > 0.80:
            fraud_probability += 0.20

        # Many recent transactions.
        if recent_count >= 6:
            fraud_probability += 0.15

        if recent_count >= 9:
            fraud_probability += 0.20

        # Unusual hours are only one signal.
        if transaction_hour <= 4:
            fraud_probability += 0.10

        # Combine signals.
        fraud_probability += random.uniform(-0.05, 0.05)

        # Keep probability between 0 and 1.
        fraud_probability = max(
            0.01,
            min(0.95, fraud_probability)
        )

        fraud = 1 if random.random() < fraud_probability else 0

        writer.writerow([
            round(amount, 2),
            transaction_hour,
            transaction_day,
            recent_count,
            round(average_amount, 2),
            round(difference, 2),
            fraud
        ])

print("Dataset generated successfully.")
print("Rows:", ROWS)
print("File:", OUTPUT_FILE)