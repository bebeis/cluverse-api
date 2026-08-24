#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
SEED_SCRIPT="$ROOT_DIR/script/aws/seed.sh"
REDIS_RESET_SCRIPT="$ROOT_DIR/script/aws/seed/reset-view-count-redis.sh"
COMMENT_SEED="$ROOT_DIR/docs/v1/ddl/test-data/06_comment_seed.sql"

assert_contains() {
  local output="$1"
  local expected="$2"
  [[ "$output" == *"$expected"* ]] || {
    echo "expected output to contain: $expected" >&2
    exit 1
  }
}

assert_not_contains() {
  local output="$1"
  local unexpected="$2"
  [[ "$output" != *"$unexpected"* ]] || {
    echo "expected output not to contain: $unexpected" >&2
    exit 1
  }
}

view_count_plan="$($SEED_SCRIPT view-count --dry-run)"
assert_contains "$view_count_plan" "05a_popular_board_post_seed.sql"
assert_contains "$view_count_plan" "view:v4:counter:*"
assert_contains "$view_count_plan" "view:v4:dedupe:*"
assert_contains "$view_count_plan" "view:v4:init:*"
assert_not_contains "$view_count_plan" "view:v2:"
assert_not_contains "$view_count_plan" "view:v3:"
assert_not_contains "$view_count_plan" "script/popularity/seed/fixture.sql"
assert_not_contains "$view_count_plan" "05c_view_count_optimistic_seed.sql"

redis_reset_plan="$($REDIS_RESET_SCRIPT --dry-run)"
assert_contains "$redis_reset_plan" "view:v4:counter:*"
assert_contains "$redis_reset_plan" "view:v4:dedupe:*"
assert_contains "$redis_reset_plan" "view:v4:init:*"
assert_not_contains "$redis_reset_plan" "view:v2:"
assert_not_contains "$redis_reset_plan" "view:v3:"
assert_not_contains "$redis_reset_plan" "FLUSHDB"

post_list_plan="$($SEED_SCRIPT post-list --dry-run)"
assert_not_contains "$post_list_plan" "script/popularity/seed/fixture.sql"
assert_contains "$post_list_plan" "Redis reset: disabled"

full_plan="$($SEED_SCRIPT full --dry-run)"
assert_contains "$full_plan" "06_comment_seed.sql"
assert_contains "$full_plan" "Redis reset: enabled"
assert_not_contains "$full_plan" "script/popularity/seed/fixture.sql"

comment_seed_sql="$(cat "$COMMENT_SEED")"
assert_contains "$comment_seed_sql" "DELETE FROM post_comment_activity"
assert_contains "$comment_seed_sql" "INSERT INTO post_comment_activity"

echo "seed plan tests passed"
