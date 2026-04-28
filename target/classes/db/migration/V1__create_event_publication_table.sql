CREATE TABLE event_publication (
    id uuid NOT NULL,
    completion_date timestamp(6) with time zone,
    event_type varchar(255),
    listener_id varchar(255),
    publication_date timestamp(6) with time zone,
    serialized_event varchar(255),
    CONSTRAINT event_publication_pkey PRIMARY KEY (id)
);

CREATE INDEX event_publication_completion_date_publication_date_idx
    ON event_publication (completion_date, publication_date);

CREATE INDEX event_publication_serialized_event_listener_id_completion_date_idx
    ON event_publication (serialized_event, listener_id, completion_date);
