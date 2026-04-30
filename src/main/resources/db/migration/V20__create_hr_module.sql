CREATE TABLE employee_functions (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(150) NOT NULL,
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(100) NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by  VARCHAR(100) NOT NULL
);

CREATE TABLE employees (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_number  VARCHAR(50)  NOT NULL UNIQUE,
    full_name        VARCHAR(200) NOT NULL,
    phone            VARCHAR(50),
    email            VARCHAR(150),
    id_number        VARCHAR(100) UNIQUE,
    function_id      UUID REFERENCES employee_functions (id),
    status           VARCHAR(30)  NOT NULL
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'TERMINATED')),
    hire_date        DATE,
    termination_date DATE,
    base_salary      NUMERIC(15, 2),
    currency         VARCHAR(3)   NOT NULL DEFAULT 'MZN',
    notes            TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(100) NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by       VARCHAR(100) NOT NULL,
    deleted_at       TIMESTAMPTZ,
    deleted_by       VARCHAR(100)
);

CREATE TABLE salary_payments (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    employee_id   UUID           NOT NULL REFERENCES employees (id),
    period_year   INT            NOT NULL,
    period_month  INT            NOT NULL CHECK (period_month BETWEEN 1 AND 12),
    gross_amount  NUMERIC(15, 2) NOT NULL CHECK (gross_amount > 0),
    net_amount    NUMERIC(15, 2) NOT NULL CHECK (net_amount > 0),
    paid_amount   NUMERIC(15, 2) NOT NULL CHECK (paid_amount > 0),
    currency      VARCHAR(3)     NOT NULL DEFAULT 'MZN',
    payment_date  DATE           NOT NULL,
    payment_method VARCHAR(30)
        CHECK (payment_method IN ('BANK_TRANSFER', 'CASH', 'MOBILE_MONEY', 'OTHER')),
    reference     VARCHAR(100),
    status        VARCHAR(30)    NOT NULL
        CHECK (status IN ('PAID', 'CANCELLED')),
    notes         TEXT,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    created_by    VARCHAR(100)   NOT NULL,
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_by    VARCHAR(100)   NOT NULL,
    CONSTRAINT uk_salary_payments_employee_period UNIQUE (employee_id, period_year, period_month)
);

CREATE INDEX idx_employee_functions_code ON employee_functions (code);
CREATE INDEX idx_employees_number ON employees (employee_number);
CREATE INDEX idx_employees_status ON employees (status);
CREATE INDEX idx_employees_function ON employees (function_id);
CREATE INDEX idx_salary_payments_employee ON salary_payments (employee_id);
CREATE INDEX idx_salary_payments_period ON salary_payments (period_year, period_month);
CREATE INDEX idx_salary_payments_status ON salary_payments (status);
