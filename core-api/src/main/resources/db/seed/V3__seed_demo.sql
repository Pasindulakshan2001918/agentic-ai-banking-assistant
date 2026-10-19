-- Demo seed data. All names, numbers and limits are FICTIONAL demo values.
-- Have a native speaker review the Sinhala/Tamil strings before you demo.

-- ---------- Customers (fixed UUIDs so tests can reference them) --------
INSERT INTO customers (id, cif, full_name, mobile, email, preferred_language) VALUES
 ('11111111-1111-1111-1111-111111111111','CIF0001','Nimal Perera',  '0771234567','nimal@example.com','SI'),
 ('22222222-2222-2222-2222-222222222222','CIF0002','Kavitha Rajan', '0779876543','kavitha@example.com','TA'),
 ('33333333-3333-3333-3333-333333333333','CIF0003','Ruwan Silva',   '0712223344','ruwan@example.com','EN');

INSERT INTO accounts (id, customer_id, account_no, product_type, nickname, balance) VALUES
 ('a1111111-0000-0000-0000-000000000001','11111111-1111-1111-1111-111111111111','1000000001','SAVINGS','Main savings', 250000.00),
 ('a1111111-0000-0000-0000-000000000002','11111111-1111-1111-1111-111111111111','2000000001','CURRENT','Current',      85000.00),
 ('a2222222-0000-0000-0000-000000000001','22222222-2222-2222-2222-222222222222','1000000002','SAVINGS','Savings',      120000.00),
 ('a3333333-0000-0000-0000-000000000001','33333333-3333-3333-3333-333333333333','1000000003','SAVINGS','Savings',       30000.00);

INSERT INTO cards (id, customer_id, card_type, card_last4, nickname, credit_limit, outstanding_balance, min_due, statement_due_date) VALUES
 ('c1111111-0000-0000-0000-000000000001','11111111-1111-1111-1111-111111111111','CREDIT','4417','Visa Gold', 300000, 45250.00, 4525.00, CURRENT_DATE + 6),
 ('c2222222-0000-0000-0000-000000000001','22222222-2222-2222-2222-222222222222','CREDIT','9021','Visa Classic', 150000, 12000.00, 1200.00, CURRENT_DATE + 12);

INSERT INTO loans (id, customer_id, loan_no, product_type, principal, outstanding, installment_amount, next_due_date) VALUES
 ('d1111111-0000-0000-0000-000000000001','11111111-1111-1111-1111-111111111111','LN00001','LOAN', 1000000, 640000, 28500.00, CURRENT_DATE + 4);

INSERT INTO payees (customer_id, nickname, beneficiary_name, bank_code, bank_name, account_no, added_at) VALUES
 ('11111111-1111-1111-1111-111111111111','Amma',       'K. Perera',      '7010','Demo Bank',      '5550001111', now() - interval '90 days'),
 ('11111111-1111-1111-1111-111111111111','Landlord',   'S. Fernando',    '7135','Other Demo Bank','5550002222', now() - interval '3 hours');  -- inside cooling window

-- ---------- Billers ---------------------------------------------------
INSERT INTO billers (id, code, name_en, name_si, name_ta, category, reference_label, aliases) VALUES
 ('b0000000-0000-0000-0000-000000000001','CEB',    'Ceylon Electricity Board', 'ලංකා විදුලිබල මණ්ඩලය', 'இலங்கை மின்சார சபை', 'ELECTRICITY','Account number','{ceb,electricity,current bill,light bill,විදුලි,மின்சாரம்}'),
 ('b0000000-0000-0000-0000-000000000002','NWSDB',  'National Water Supply & Drainage Board','ජාතික ජල සම්පාදන හා ජලාපවහන මණ්ඩලය','தேசிய நீர் வழங்கல் மற்றும் வடிகாலமைப்புச் சபை','WATER','Account number','{water,nwsdb,වතුර,ජල,தண்ணீர்}'),
 ('b0000000-0000-0000-0000-000000000003','DIALOG','Dialog Axiata','ඩයලොග්','டயலொக்','TELECOM','Mobile / account number','{dialog,mobile bill,phone bill,ඩයලොග්,டயலொக்}'),
 ('b0000000-0000-0000-0000-000000000004','SLT',   'SLT-Mobitel','ශ්‍රී ලංකා ටෙලිකොම්','ஸ்ரீ லங்கா டெலிகொம்','TELECOM','Telephone number','{slt,mobitel,telephone,broadband}');

INSERT INTO registered_billers (id, customer_id, biller_id, reference_no, nickname) VALUES
 ('e1111111-0000-0000-0000-000000000001','11111111-1111-1111-1111-111111111111','b0000000-0000-0000-0000-000000000001','0123456789','Home electricity'),
 ('e1111111-0000-0000-0000-000000000002','11111111-1111-1111-1111-111111111111','b0000000-0000-0000-0000-000000000003','0771234567','My Dialog');

INSERT INTO bill_dues (registered_biller_id, amount_due, due_date) VALUES
 ('e1111111-0000-0000-0000-000000000001', 5000.00, CURRENT_DATE + 3),
 ('e1111111-0000-0000-0000-000000000002', 1850.00, CURRENT_DATE + 8);

-- ---------- Policy: product matrix & OTP mandates (DEMO values) ---------
INSERT INTO product_rules
 (action_type, source_product, rail, min_amount, max_per_txn, max_daily, otp_threshold_per_txn, otp_threshold_daily, always_otp, fee_flat, fee_pct, notes) VALUES
 ('FUND_TRANSFER',        'ANY',    'INTERNAL', 1,   1000000, 2000000, 25000, 50000, false,  0, 0,      'Own / same-bank transfers'),
 ('FUND_TRANSFER',        'ANY',    'JUSTPAY',  1,    500000, 1000000, 10000, 25000, false, 25, 0,      'Inter-bank via JustPay (demo limits)'),
 ('BILL_PAYMENT',         'ANY',    'INTERNAL', 1,    200000,  500000, 20000, 50000, false,  0, 0,      NULL),
 ('MOBILE_RELOAD',        'ANY',    'INTERNAL', 50,    10000,   30000, 5000,  15000, false,  0, 0,      NULL),
 ('CREDIT_CARD_PAYMENT',  'ANY',    'INTERNAL', 1,    500000, 1000000, 50000, 100000,false,  0, 0,      NULL),
 ('LOAN_PAYMENT',         'ANY',    'INTERNAL', 1,   1000000, 2000000, 50000, 100000,false,  0, 0,      NULL),
 ('CARD_BLOCK',           'ANY',    'INTERNAL', 0,   NULL,    NULL,    NULL,  NULL,   false, 0, 0,      'Blocking is protective: no OTP'),
 ('CARD_UNBLOCK',         'ANY',    'INTERNAL', 0,   NULL,    NULL,    NULL,  NULL,   true,  0, 0,      'Step-up: reversing a block'),
 ('CARD_ONLINE_TOGGLE',   'ANY',    'INTERNAL', 0,   NULL,    NULL,    NULL,  NULL,   false, 0, 0,      NULL),
 ('CARD_PIN_RESET',       'ANY',    'INTERNAL', 0,   NULL,    NULL,    NULL,  NULL,   true,  0, 0,      'Sensitive'),
 ('FD_OPEN',              'ANY',    'INTERNAL', 25000,NULL,   NULL,    NULL,  NULL,   true,  0, 0,      NULL),
 ('SAVINGS_OPEN',         'ANY',    'INTERNAL', 1000, NULL,   NULL,    NULL,  NULL,   false, 0, 0,      NULL),
 ('SERVICE_REQUEST',      'ANY',    'INTERNAL', 0,   NULL,    NULL,    NULL,  NULL,   false, 0, 0,      NULL);

-- ---------- Branches / ATMs (approximate demo coordinates) --------------
INSERT INTO branches (kind, name_en, name_si, name_ta, city, address, latitude, longitude, opening_hours, services) VALUES
 ('BRANCH','Colombo Fort Branch','කොළඹ කොටුව ශාඛාව','கொழும்பு கோட்டை கிளை','Colombo','Demo address, Fort, Colombo 01', 6.9344, 79.8428,'Mon-Fri 09:00-16:00','{deposits,loans,pawning,cheque_book}'),
 ('ATM',   'Colombo Fort ATM',   'කොළඹ කොටුව ATM','கொழும்பு கோட்டை ATM','Colombo','Demo address, Fort, Colombo 01', 6.9350, 79.8420,'24 hours','{cash_withdrawal,balance}'),
 ('BRANCH','Kandy Branch','මහනුවර ශාඛාව','கண்டி கிளை','Kandy','Demo address, Kandy', 7.2906, 80.6337,'Mon-Fri 09:00-16:00','{deposits,loans,cheque_book}'),
 ('BRANCH','Matara Branch','මාතර ශාඛාව','மாத்தறை கிளை','Matara','Demo address, Matara', 5.9549, 80.5550,'Mon-Fri 09:00-16:00','{deposits,loans,pawning}'),
 ('ATM',   'Galle ATM','ගාල්ල ATM','காலி ATM','Galle','Demo address, Galle', 6.0329, 80.2168,'24 hours','{cash_withdrawal,balance}');

-- ---------- FAQs -------------------------------------------------------
INSERT INTO faqs (category, question_en, question_si, question_ta, answer_en, answer_si, answer_ta, keywords) VALUES
 ('OTP','Why do I need an OTP?','මට OTP අවශ්‍ය වන්නේ ඇයි?','எனக்கு ஏன் OTP தேவை?',
  'An OTP is required for high-value or sensitive actions to keep your money safe.',
  'ඔබේ මුදල් ආරක්ෂා කිරීම සඳහා වැඩි වටිනාකමැති හෝ සංවේදී ක්‍රියා සඳහා OTP අවශ්‍ය වේ.',
  'உங்கள் பணத்தைப் பாதுகாக்க அதிக மதிப்புள்ள அல்லது முக்கியமான செயல்களுக்கு OTP தேவை.','{otp,security}'),
 ('CARDS','How do I block my card?','මගේ කාඩ්පත අවහිර කරන්නේ කෙසේද?','என் அட்டையை எவ்வாறு முடக்குவது?',
  'Just tell me "block my card" and I will guide you. Blocking does not need an OTP.',
  '"මගේ කාඩ්පත අවහිර කරන්න" යැයි පවසන්න, මම ඔබට මඟ පෙන්වන්නම්.',
  '"என் அட்டையை முடக்கு" என்று சொல்லுங்கள், நான் வழிகாட்டுகிறேன்.','{card,block,lost}');

INSERT INTO promotions (title_en, title_si, title_ta, body_en, body_si, body_ta, valid_from, valid_to) VALUES
 ('Fixed Deposit Bonus','ස්ථාවර තැන්පතු ප්‍රසාද','நிலையான வைப்பு போனஸ்',
  'Open a 12-month FD this month and get +0.5% interest (demo promotion).',
  'මෙම මාසයේ මාස 12ක ස්ථාවර තැන්පතුවක් ආරම්භ කර +0.5% පොලී ලබාගන්න (demo).',
  'இந்த மாதம் 12 மாத நிலையான வைப்பைத் தொடங்கி +0.5% வட்டி பெறுங்கள் (demo).',
  CURRENT_DATE - 5, CURRENT_DATE + 25);

