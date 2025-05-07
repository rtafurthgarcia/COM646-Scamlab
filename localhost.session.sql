--- quick overview of all conversations of the day ---
SELECT start_of_conversation AS start_time, secondary_id, testing_scenario, states.name AS state, strategies.name AS strategy, bothasbeenunmasked, invalidated
FROM conversations
INNER JOIN states ON states.id = conversations.state_id
INNER JOIN strategies ON strategies.id = conversations.strategy_id
WHERE testing_day = DATE '2025-04-02'
ORDER BY start_of_conversation DESC;

--- specific conversation ---
SELECT messages.creation, message, username, roles.name AS role FROM messages
INNER JOIN conversations ON conversations.id = messages.conversation_id
INNER JOIN players ON players.id = messages.player_id
INNER JOIN participations ON participations.conversation_id = conversations.id AND participations.player_id = messages.player_id
INNER JOIN roles ON roles.id = participations.role_id
WHERE conversations.secondary_id = '$1';

--- specific conversation by USERNAME---
SELECT conversations.id, messages.creation, message, username, roles.name AS role FROM messages
INNER JOIN conversations ON conversations.id = messages.conversation_id
INNER JOIN players ON players.id = messages.player_id
INNER JOIN participations ON participations.conversation_id = conversations.id AND participations.player_id = messages.player_id
INNER JOIN roles ON roles.id = participations.role_id
WHERE conversations.id IN 
(
    SELECT C2.id AS role FROM conversations C2
    INNER JOIN messages M2 ON M2.conversation_id = C2.id
    INNER JOIN players P2 ON P2.id = M2.player_id
    INNER JOIN participations PP2 ON PP2.conversation_id = C2.id AND PP2.player_id = M2.player_id
    WHERE username = '$1'
)
ORDER BY messages.creation;

--- invalidate in case players didnt follow the rulz
UPDATE conversations SET invalidated = true 
WHERE conversations.secondary_id = '$1' AND '$2' = 'OK'