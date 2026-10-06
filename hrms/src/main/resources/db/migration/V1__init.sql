CREATE TABLE employees (
    id BIGSERIAL PRIMARY KEY,
    employee_code VARCHAR(20) NOT NULL UNIQUE,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    user_role VARCHAR(20) NOT NULL,
    department VARCHAR(80) NOT NULL,
    designation VARCHAR(80) NOT NULL,
    phone VARCHAR(30),
    joining_date DATE NOT NULL,
    basic_salary NUMERIC(12, 2) NOT NULL,
    annual_leave_balance INTEGER NOT NULL,
    active BOOLEAN NOT NULL
);

CREATE TABLE leave_requests (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees (id),
    leave_type VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reviewer_id BIGINT REFERENCES employees (id),
    reviewed_at TIMESTAMP(6),
    applied_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_leave_employee ON leave_requests (employee_id);

CREATE TABLE attendance_records (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees (id),
    work_date DATE NOT NULL,
    check_in TIMESTAMP(6),
    check_out TIMESTAMP(6),
    CONSTRAINT uq_attendance_employee_day UNIQUE (employee_id, work_date)
);

CREATE INDEX idx_attendance_employee ON attendance_records (employee_id);

CREATE TABLE payslips (
    id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees (id),
    pay_year INTEGER NOT NULL,
    pay_month INTEGER NOT NULL,
    basic_salary NUMERIC(12, 2) NOT NULL,
    house_rent_allowance NUMERIC(12, 2) NOT NULL,
    special_allowance NUMERIC(12, 2) NOT NULL,
    provident_fund NUMERIC(12, 2) NOT NULL,
    professional_tax NUMERIC(12, 2) NOT NULL,
    loss_of_pay NUMERIC(12, 2) NOT NULL,
    unpaid_days INTEGER NOT NULL,
    gross_pay NUMERIC(12, 2) NOT NULL,
    net_pay NUMERIC(12, 2) NOT NULL,
    generated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_payslip_period UNIQUE (employee_id, pay_year, pay_month)
);

CREATE INDEX idx_payslip_employee ON payslips (employee_id);
