-- DROP TABLE IF EXISTS test;
-- DROP TABLE IF EXISTS notification;

-- 테스트용 테이블
CREATE TABLE test
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP
);


-- 알림 테이블
CREATE TABLE notification
(
    noti_id     BIGSERIAL PRIMARY KEY,
    type        VARCHAR(20)  NOT NULL, -- FEEDBACK, QNA, UPDATE
    department  VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    sender_id   BIGINT       NOT NULL, -- 알림 발신자
    receiver_id BIGINT NULL,           -- 알림 수신자
    company_id  BIGINT       NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP
);


-- 읽음 상태 테이블
CREATE TABLE notification_status
(
    id         BIGSERIAL PRIMARY KEY,
    noti_id    BIGINT    NOT NULL,
    reader_id  BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT uq_notification_read UNIQUE (noti_id, reader_id),
    FOREIGN KEY (noti_id) REFERENCES notification (noti_id) ON DELETE CASCADE
);

-- CREATE INDEX idx_notification_status_reader ON notification_status (reader_id, noti_id);
