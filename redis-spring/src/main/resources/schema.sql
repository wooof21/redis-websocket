DROP TABLE IF EXISTS chat_message;
CREATE TABLE chat_message (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   room VARCHAR (255) NOT NULL,
   username VARCHAR (255) NOT NULL,
   message TEXT NOT NULL,
   timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- create an index on room and timestamp for efficient querying
CREATE INDEX idx_chat_message_room_timestamp
    ON chat_message(room, timestamp DESC);