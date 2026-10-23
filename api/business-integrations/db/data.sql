INSERT IGNORE INTO sso_user_mapping (outer_user_id, local_user_id) VALUES
('ys-user-001', 'local-user-1001'),
('13800000000', 'local-user-1002');

INSERT IGNORE INTO bill_trace_node (id, bill_type, bill_no, name, status) VALUES
('SO-1001', 'SALES_ORDER', 'SO-1001', '销售订单', '已审核'),
('MO-1001', 'PRODUCTION_ORDER', 'MO-1001', '生产订单', '已审核'),
('QT-1001', 'QUALITY_TEST', 'QT-1001', '质检单', '已审核'),
('RIS-1001', 'RECEIVE_IN_STOCK', 'RIS-1001', '采购入库单', '已审核'),
('BC001', 'BARCODE', 'BC001', '条码', '已审核');

INSERT IGNORE INTO bill_trace_edge (from_id, to_id) VALUES
('SO-1001', 'MO-1001'),
('MO-1001', 'QT-1001'),
('QT-1001', 'RIS-1001'),
('RIS-1001', 'BC001');

INSERT IGNORE INTO barcode_flow_record
(barcode, business_name, operation_type, bill_no, warehouse, business_time, record_status) VALUES
('BC001', '标签打印', 'PRINT', 'LABEL-1001', '一号仓', '2026-10-01 09:00:00', 'SUCCESS'),
('BC001', '来料入库', 'INBOUND', 'RK-1001', '一号仓', '2026-10-01 10:00:00', 'SUCCESS'),
('BC001', '生产领料', 'ISSUE', 'LL-1001', '一号仓', '2026-10-02 09:00:00', 'SUCCESS'),
('BC001', '成品入库', 'INBOUND', 'CPRK-1001', '二号仓', '2026-10-03 15:00:00', 'SUCCESS'),
('BC001', '销售出库', 'OUTBOUND', 'CK-1001', '二号仓', '2026-10-04 11:00:00', 'SUCCESS'),
('BC002', '标签打印', 'PRINT', 'LABEL-1002', '一号仓', '2026-10-01 09:10:00', 'SUCCESS'),
('BC002', '来料入库', 'INBOUND', 'RK-1002', '一号仓', '2026-10-01 10:20:00', 'SUCCESS');
