#!/bin/bash

# ============================================================
#  MoneyWise — Default Data Seeder (Docker)
#  Seeds the PostgreSQL database running inside Docker.
#
#  Usage:
#    chmod +x seed_data.sh
#    ./seed_data.sh
# ============================================================

set -euo pipefail

# --------------- Configuration ---------------
CONTAINER_NAME="postgres-db"
DB_NAME="moneywise"
DB_USER="vaibhaw"

# --------------- Colours for output ---------------
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

info()  { echo -e "${GREEN}[✔] $1${NC}"; }
warn()  { echo -e "${YELLOW}[!] $1${NC}"; }
error() { echo -e "${RED}[✖] $1${NC}"; exit 1; }

# --------------- Pre-flight check ---------------
if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    error "Docker container '${CONTAINER_NAME}' is not running. Start it first."
fi

echo ""
echo "╔══════════════════════════════════════════════╗"
echo "║     MoneyWise — Database Seeder (Docker)     ║"
echo "║     Container: ${CONTAINER_NAME}                    ║"
echo "║     Database:  ${DB_NAME}                     ║"
echo "╚══════════════════════════════════════════════╝"
echo ""

# Helper to run psql inside the container
run_sql() {
    docker exec -i "$CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1
}

# --------------- Test connection ---------------
echo "SELECT 1;" | run_sql > /dev/null 2>&1 || error "Cannot connect to database inside container."
info "Connected to database successfully."

# ============================================================
#  1. Seed Category Types  (app_category_type)
# ============================================================
warn "Seeding category types..."

run_sql <<'SQL'
INSERT INTO app_category_type (category__type_name)
VALUES
    ('Income'),
    ('Expense'),
    ('Transfer')
ON CONFLICT DO NOTHING;
SQL

info "Category types seeded."

# ============================================================
#  2. Seed Categories  (app_category)
# ============================================================
warn "Seeding categories..."

run_sql <<'SQL'
-- Income categories  (category_type_id = 1)
INSERT INTO app_category (category_name, category_type_id, category_icon, is_active)
VALUES
    ('Salary',          1, '💰', true),
    ('Freelance',       1, '💻', true),
    ('Investments',     1, '📈', true),
    ('Gifts',           1, '🎁', true),
    ('Other Income',    1, '💵', true)
ON CONFLICT DO NOTHING;

-- Expense categories  (category_type_id = 2)
INSERT INTO app_category (category_name, category_type_id, category_icon, is_active)
VALUES
    ('Food & Dining',   2, '🍔', true),
    ('Transport',       2, '🚗', true),
    ('Shopping',        2, '🛍️', true),
    ('Rent',            2, '🏠', true),
    ('Utilities',       2, '💡', true),
    ('Entertainment',   2, '🎬', true),
    ('Healthcare',      2, '🏥', true),
    ('Education',       2, '📚', true),
    ('Groceries',       2, '🛒', true),
    ('Subscriptions',   2, '📱', true),
    ('Travel',          2, '✈️', true),
    ('Personal Care',   2, '💇', true),
    ('Insurance',       2, '🛡️', true),
    ('Other Expense',   2, '💸', true)
ON CONFLICT DO NOTHING;

-- Transfer categories  (category_type_id = 3)
INSERT INTO app_category (category_name, category_type_id, category_icon, is_active)
VALUES
    ('Bank Transfer',       3, '🏦', true),
    ('UPI Transfer',        3, '📲', true),
    ('Cash Withdrawal',     3, '🏧', true),
    ('Other Transfer',      3, '🔄', true)
ON CONFLICT DO NOTHING;
SQL

info "Categories seeded."

# ============================================================
#  3. Seed Demo User  (app_users)
# ============================================================
warn "Seeding demo user..."

run_sql <<'SQL'
INSERT INTO app_users (user_name, password, email)
VALUES
    ('demo_user', 'demo1234', 'demo@moneywise.app')
ON CONFLICT DO NOTHING;
SQL

info "Demo user seeded.  (username: demo_user / password: demo1234)"

# ============================================================
#  4. Seed Sample Transactions  (app_transaction)
# ============================================================
warn "Seeding sample transactions..."

run_sql <<'SQL'
DO $$
DECLARE
    v_user_id INTEGER;
BEGIN
    SELECT id INTO v_user_id FROM app_users WHERE user_name = 'demo_user' LIMIT 1;

    IF v_user_id IS NULL THEN
        RAISE NOTICE 'Demo user not found — skipping transactions.';
        RETURN;
    END IF;

    -- Income transactions
    INSERT INTO app_transaction
        (user_id, transaction_amount, transaction_category_id, transaction_message,
         transaction_date, transaction_date_int, is_modifiy, transaction_modification_count)
    VALUES
        (v_user_id, 50000, 1, 'June salary',            '2026-06-01', 20260601, 0, 0),
        (v_user_id, 15000, 2, 'Freelance web project',   '2026-06-05', 20260605, 0, 0),
        (v_user_id,  2000, 4, 'Birthday gift',           '2026-06-15', 20260615, 0, 0);

    -- Expense transactions
    INSERT INTO app_transaction
        (user_id, transaction_amount, transaction_category_id, transaction_message,
         transaction_date, transaction_date_int, is_modifiy, transaction_modification_count)
    VALUES
        (v_user_id,   450, 6,  'Lunch with friends',     '2026-06-02', 20260602, 0, 0),
        (v_user_id,  1200, 7,  'Cab rides this week',    '2026-06-03', 20260603, 0, 0),
        (v_user_id,  3500, 8,  'New headphones',         '2026-06-07', 20260607, 0, 0),
        (v_user_id, 12000, 9,  'Monthly rent',           '2026-06-01', 20260601, 0, 0),
        (v_user_id,  1800, 10, 'Electricity bill',       '2026-06-10', 20260610, 0, 0),
        (v_user_id,   500, 11, 'Movie tickets',          '2026-06-12', 20260612, 0, 0),
        (v_user_id,  2200, 14, 'Weekly groceries',       '2026-06-08', 20260608, 0, 0),
        (v_user_id,   199, 15, 'Netflix subscription',   '2026-06-01', 20260601, 0, 0),
        (v_user_id,  5000, 16, 'Weekend trip',           '2026-06-20', 20260620, 0, 0);

    -- Transfer transactions
    INSERT INTO app_transaction
        (user_id, transaction_amount, transaction_category_id, transaction_message,
         transaction_date, transaction_date_int, is_modifiy, transaction_modification_count)
    VALUES
        (v_user_id, 10000, 20, 'Savings transfer',       '2026-06-05', 20260605, 0, 0),
        (v_user_id,  3000, 21, 'UPI to friend',          '2026-06-14', 20260614, 0, 0);

    RAISE NOTICE 'Sample transactions inserted for user_id=%', v_user_id;
END $$;
SQL

info "Sample transactions seeded."

# ============================================================
#  Done!
# ============================================================
echo ""
echo "╔══════════════════════════════════════════════╗"
echo "║  ✅  All default data seeded successfully!   ║"
echo "╚══════════════════════════════════════════════╝"
echo ""
info "Demo credentials →  username: demo_user  |  password: demo1234"
echo ""
