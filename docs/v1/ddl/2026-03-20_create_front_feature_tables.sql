CREATE TABLE calendar_event (
    calendar_event_id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT NULL,
    category VARCHAR(20) NOT NULL,
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,
    location VARCHAR(255) NULL,
    all_day BIT NOT NULL,
    visibility VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT NOW(),
    updated_at DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (calendar_event_id)
);

CREATE TABLE campus_event (
    campus_event_id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    host VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    location VARCHAR(255) NULL,
    thumbnail_image_url VARCHAR(500) NULL,
    summary TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT NOW(),
    updated_at DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (campus_event_id)
);

CREATE TABLE notification (
    notification_id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NULL,
    excerpt VARCHAR(255) NULL,
    is_read BIT NOT NULL DEFAULT b'0',
    target_url VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT NOW(),
    updated_at DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (notification_id)
);

CREATE TABLE notification_preference (
    member_id BIGINT NOT NULL,
    comments BIT NOT NULL DEFAULT b'1',
    groups BIT NOT NULL DEFAULT b'1',
    announcements BIT NOT NULL DEFAULT b'1',
    follows BIT NOT NULL DEFAULT b'1',
    marketing BIT NOT NULL DEFAULT b'0',
    PRIMARY KEY (member_id)
);

CREATE TABLE member_report (
    member_report_id BIGINT NOT NULL AUTO_INCREMENT,
    reporter_id BIGINT NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT NOT NULL,
    reason_code VARCHAR(30) NOT NULL,
    detail TEXT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT NOW(),
    updated_at DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (member_report_id)
);

CREATE TABLE member_report_evidence_image (
    member_report_id BIGINT NOT NULL,
    image_url VARCHAR(500) NULL
);
