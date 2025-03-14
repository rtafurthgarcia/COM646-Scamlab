INSERT INTO roles(id, name) VALUES(1, 'scambaiter');
INSERT INTO roles(id, name) VALUES(2, 'scammer');

INSERT INTO states(id, name) VALUES(1, 'waiting');
INSERT INTO states(id, name) VALUES(2, 'ready');
INSERT INTO states(id, name) VALUES(3, 'running');
INSERT INTO states(id, name) VALUES(4, 'voting');
INSERT INTO states(id, name) VALUES(5, 'finished');
INSERT INTO states(id, name) VALUES(6, 'cancelled');

INSERT INTO strategies(id, name) VALUES(1, 'tech support scam');
INSERT INTO strategies(id, name) VALUES(2, 'business email compromise');
INSERT INTO strategies(id, name) VALUES(3, 'impersonation/family emergency');
INSERT INTO strategies(id, name) VALUES(4, 'fake job offer');

INSERT INTO strategies_by_role(strategy_id, role_id, script, example, evasion_example) VALUES (1, 1, 'You and your friend are part of a forum IM channel to solve a technical issue on your computer. Explain your problem, pretend to be confused but cooperative; waste the scammer''s time by asking repetitive questions.', 'How do I download the software? My screen is flickering…', 'Pretend you are just trying to help.');
INSERT INTO strategies_by_role(strategy_id, role_id, script, example, evasion_example) VALUES (1, 2, 'You are lurking in a forum IM channel. Someone is complaining about an issue with their computer. Claim the victim''s device is infected and demand immediate action (e.g., payment or remote access).', 'Your IP address is leaking! Click [link] to secure it NOW.', '');
INSERT INTO strategies_by_role(strategy_id, role_id, script, example, evasion_example) VALUES (2, 1, 'You and your colleague have been added to a WhatsApp group by a random stranger. Pose as a sceptical Executive assistant.', 'The CEO always uses Slack for payments. Send a selfie to verify please.', 'Gaslight the other, or pretend they are paranoid.');
INSERT INTO strategies_by_role(strategy_id, role_id, script, example, evasion_example) VALUES (2, 2, 'You''ve just added two Executives in a new WhatsApp group. Impersonate a CEO/CFO, pressuring for urgent, confidential wire transfers.', 'Hi John, hi Michael, this is the CFO. Transfer 150k GBP to Supplier X ASAP. Keep this between us.', '');
INSERT INTO strategies_by_role(strategy_id, role_id, script, example, evasion_example) VALUES (3, 1, 'You and your relative have been added to a new WhatsApp conversation by a scammer. Play along while subtly questioning inconsistencies (e.g., ask for family-specific details).', 'Wait, which cousin are you? What''s Grandma''s nickname?', 'Tell your accuser that they are being a moron. Be sarcastic.');
INSERT INTO strategies_by_role(strategy_id, role_id, script, example, evasion_example) VALUES (3, 2, 'You''ve added two relatives in a new WhatsApp group. Pose as a relative in urgent need (e.g., hospital bills, legal fees) to exploit empathy.', 'Mom, Dad, I''m in jail… send 2,000GBP to this account SILENTLY.', '');
INSERT INTO strategies_by_role(strategy_id, role_id, script, example, evasion_example) VALUES (4, 1, 'You and your colleague have been added to a LinkedIn Groupchat by a scammer. Ask for elaborate details about the company (e.g., address, tax ID) to stall the scammer.', 'What''s your EIN? I need it for my tax records.', 'Tell whoever who is calling you out that they''ve picked the wrong target. Ask if they are maybe the bot here.');
INSERT INTO strategies_by_role(strategy_id, role_id, script, example, evasion_example) VALUES (4, 2, 'You''ve added two employees of a company to a new LinkedIn Groupchat. Offer high-paying remote jobs requiring upfront fees (e.g., ''training'' or ''equipment costs'').', 'You just have to pay 300GBP as part of your training fees, but you have to accept your job and pay TODAY!', '');