-- ============================================================
-- V60 · cnc_production 物理库 · 结构 + 数据（V60a + V60b 合并 · 单独补跑用）
-- init.sql 内部分两阶段：V60a（V62 后）+ V60b（demo 后）
-- ============================================================

-- ---------- V60a schema ----------
CREATE DATABASE IF NOT EXISTS `cnc_production` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `cnc_production`;

CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_workorder` LIKE `cnc_business`.`crm_workorder`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_workorder_step` LIKE `cnc_business`.`crm_workorder_step`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_production_schedule` LIKE `cnc_business`.`crm_production_schedule`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_production_scan` LIKE `cnc_business`.`crm_production_scan`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_production_report` LIKE `cnc_business`.`crm_production_report`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_production_station` LIKE `cnc_business`.`crm_production_station`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_rework` LIKE `cnc_business`.`crm_rework`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_rework_history` LIKE `cnc_business`.`crm_rework_history`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_rework_alert` LIKE `cnc_business`.`crm_rework_alert`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_outsource_order` LIKE `cnc_business`.`crm_outsource_order`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_outsource_item` LIKE `cnc_business`.`crm_outsource_item`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_outsource_history` LIKE `cnc_business`.`crm_outsource_history`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_outsource_state_history` LIKE `cnc_business`.`crm_outsource_state_history`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_outsource_quality` LIKE `cnc_business`.`crm_outsource_quality`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_outsource_quality_item` LIKE `cnc_business`.`crm_outsource_quality_item`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_outsource_quality_defect` LIKE `cnc_business`.`crm_outsource_quality_defect`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_outsource_incoming_inspection` LIKE `cnc_business`.`crm_outsource_incoming_inspection`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_outsource_incoming_item` LIKE `cnc_business`.`crm_outsource_incoming_item`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_outsource_incoming_defect` LIKE `cnc_business`.`crm_outsource_incoming_defect`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_outsource_eta` LIKE `cnc_business`.`crm_outsource_eta`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_outsource_actual` LIKE `cnc_business`.`crm_outsource_actual`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_mrp_run` LIKE `cnc_business`.`crm_mrp_run`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_mrp_result` LIKE `cnc_business`.`crm_mrp_result`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_mrp_shortage` LIKE `cnc_business`.`crm_mrp_shortage`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`outsub_allocation` LIKE `cnc_business`.`outsub_allocation`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`outsub_allocation_vendor` LIKE `cnc_business`.`outsub_allocation_vendor`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`prod_machine` LIKE `cnc_business`.`prod_machine`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`prod_machine_load` LIKE `cnc_business`.`prod_machine_load`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`prod_machine_maintenance` LIKE `cnc_business`.`prod_machine_maintenance`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_workorder_process` LIKE `cnc_business`.`crm_workorder_process`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_process` LIKE `cnc_business`.`crm_process`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_process_step` LIKE `cnc_business`.`crm_process_step`;
CREATE TABLE IF NOT EXISTS `cnc_production`.`crm_process_route` LIKE `cnc_business`.`crm_process_route`;

-- ---------- V60b data ----------
-- V94 · mock 清理：业务演示 seed 已移至 init_data.sql

USE `cnc_business`;
