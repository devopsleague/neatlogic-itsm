ALTER TABLE `process_comment_template`
DROP INDEX `uk_name`,
  ADD UNIQUE INDEX `uk_name_fcu` (`name` ASC, `fcd`) VISIBLE;