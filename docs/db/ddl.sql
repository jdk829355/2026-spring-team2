CREATE TABLE "users" (
                        "id"        BIGINT          NOT NULL,
                        "name"      VARCHAR(255)    NOT NULL,
                        "email"     VARCHAR(255)    NOT NULL,
                        "password"  VARCHAR(255)    NOT NULL,
                        "role"      VARCHAR(20)     NOT NULL
);

CREATE TABLE "animation" (
                             "id"        BIGINT          NOT NULL,
                             "title"     VARCHAR(255)    NOT NULL
);

CREATE TABLE "goods" (
                         "id"            BIGINT          NOT NULL,
                         "name"          VARCHAR(255)    NOT NULL,
                         "animation_id"  BIGINT          NOT NULL
);

CREATE TABLE "store" (
                         "id"            BIGINT          NOT NULL,
                         "name"          VARCHAR(255)    NOT NULL,
                         "type"          VARCHAR(255)    NOT NULL,
                         "start_date"    DATE            NULL,
                         "end_date"      DATE            NULL,
                         "address"       VARCHAR(255)    NOT NULL,
                         "lat"           DECIMAL(10, 7)  NOT NULL,
                         "lng"           DECIMAL(10, 7)  NOT NULL
);

CREATE TABLE "store_goods" (
                               "id"            BIGINT          NOT NULL,
                               "price"         INT             NOT NULL,
                               "stock"         INT             NOT NULL,
                               "image_path"    TEXT            NOT NULL,
                               "goods_id"      BIGINT          NOT NULL,
                               "store_id"      BIGINT          NOT NULL
);

CREATE TABLE "planner" (
                           "id"        BIGINT          NOT NULL,
                           "user_id"   BIGINT          NOT NULL,
                           "title"     VARCHAR(255)    NOT NULL,
                           "date"      DATE            NOT NULL
);

CREATE TABLE "planner_goods" (
                                 "id"                BIGINT  NOT NULL,
                                 "store_goods_id"    BIGINT  NOT NULL,
                                 "planner_id"        BIGINT  NOT NULL
);

CREATE TABLE "store_admin" (
                               "id"        BIGINT  NOT NULL,
                               "user_id"   BIGINT  NOT NULL,
                               "store_id"  BIGINT  NOT NULL
);

-- PK
ALTER TABLE "users"         ADD CONSTRAINT "PK_USER"          PRIMARY KEY ("id");
ALTER TABLE "animation"     ADD CONSTRAINT "PK_ANIMATION"     PRIMARY KEY ("id");
ALTER TABLE "goods"         ADD CONSTRAINT "PK_GOODS"         PRIMARY KEY ("id");
ALTER TABLE "store"         ADD CONSTRAINT "PK_STORE"         PRIMARY KEY ("id");
ALTER TABLE "store_goods"   ADD CONSTRAINT "PK_STORE_GOODS"   PRIMARY KEY ("id");
ALTER TABLE "planner"       ADD CONSTRAINT "PK_PLANNER"       PRIMARY KEY ("id");
ALTER TABLE "planner_goods" ADD CONSTRAINT "PK_PLANNER_GOODS" PRIMARY KEY ("id");
ALTER TABLE "store_admin"   ADD CONSTRAINT "PK_STORE_ADMIN"   PRIMARY KEY ("id");

-- FK
ALTER TABLE "goods"
    ADD CONSTRAINT "FK_animation_TO_goods"
        FOREIGN KEY ("animation_id") REFERENCES "animation" ("id");

ALTER TABLE "store_goods"
    ADD CONSTRAINT "FK_goods_TO_store_goods"
        FOREIGN KEY ("goods_id") REFERENCES "goods" ("id");

ALTER TABLE "store_goods"
    ADD CONSTRAINT "FK_store_TO_store_goods"
        FOREIGN KEY ("store_id") REFERENCES "store" ("id");

ALTER TABLE "planner"
    ADD CONSTRAINT "FK_user_TO_planner"
        FOREIGN KEY ("user_id") REFERENCES "users" ("id");

ALTER TABLE "planner_goods"
    ADD CONSTRAINT "FK_store_goods_TO_planner_goods"
        FOREIGN KEY ("store_goods_id") REFERENCES "store_goods" ("id");

ALTER TABLE "planner_goods"
    ADD CONSTRAINT "FK_planner_TO_planner_goods"
        FOREIGN KEY ("planner_id") REFERENCES "planner" ("id");

ALTER TABLE "store_admin"
    ADD CONSTRAINT "FK_user_TO_store_admin"
        FOREIGN KEY ("user_id") REFERENCES "users" ("id");

ALTER TABLE "store_admin"
    ADD CONSTRAINT "FK_store_TO_store_admin"
        FOREIGN KEY ("store_id") REFERENCES "store" ("id");

ALTER TABLE "store_admin"   ADD CONSTRAINT "UQ_STORE_ADMIN"     UNIQUE ("user_id", "store_id");