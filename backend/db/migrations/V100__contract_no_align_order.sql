-- V100 · 合同回款：contract_no 与 sales 订单号 order_no 统一（PRD AC-2.4）
USE `cnc_business`;

UPDATE sales_contract sc
INNER JOIN crm_order o ON o.id = sc.order_id
SET sc.contract_no = o.order_no
WHERE sc.order_id IS NOT NULL
  AND o.order_no IS NOT NULL
  AND sc.contract_no <> o.order_no;
