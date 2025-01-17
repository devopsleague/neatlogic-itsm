ALTER TABLE `process_workcenter`
ADD COLUMN `fcd` timestamp(3) NULL COMMENT '创建时间' AFTER `thead_config_hash`;

ALTER TABLE `process_workcenter`
ADD COLUMN `fcu` char(32) NULL COMMENT '创建人' AFTER `fcd`;

ALTER TABLE `process_workcenter`
ADD COLUMN `is_active` tinyint(1) NULL DEFAULT 1 COMMENT '是否禁用' AFTER `fcu`;

update `process_workcenter` set is_active = 1 ;