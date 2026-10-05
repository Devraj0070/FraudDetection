import csv
import random

random.seed(42)

OUTPUT_FILE = "ml/dataset/fraud_transactions_generated.csv"

ROWS = 1000

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

        average_amount = random.uniform(500, 5000)

        amount = random.gauss(
            average_amount,
            average_amount * 0.30
        )

        amount = max(50, amount)

        transaction_hour = random.randint(0, 23)

        transaction_day = random.randint(0, 6)

        recent_count = random.randint(0, 12)

        difference = abs(amount - average_amount)

        # Start with a neutral fraud probability.
        fraud_probability = 0.10

        # Unusual amount compared with normal behaviour.
        if difference > average_amount * 0.60:
            fraud_probability += 0.20

        # Many transactions in a short period.
        if recent_count >= 8:
            fraud_probability += 0.25

        # Some hours are more unusual,
        # but time alone does NOT mean fraud.
        if transaction_hour <= 4:
            fraud_probability += 0.10

        # Combine signals with random noise.
        fraud_probability += random.uniform(-0.10, 0.10)

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