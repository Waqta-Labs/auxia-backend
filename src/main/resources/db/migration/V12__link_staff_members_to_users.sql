-- A member of a brigade can be the FIELD_BRIGADE account that signs in to the mobile app, so the
-- app shows that account only the missions of its brigade (A_07). An account is in one brigade
-- at most. Logical reference to identity_access: not a foreign key across schemas.

ALTER TABLE resource_management.staff_members
    ADD COLUMN user_id UUID;

CREATE UNIQUE INDEX uq_staff_member_user
    ON resource_management.staff_members (user_id)
    WHERE user_id IS NOT NULL;

COMMENT ON COLUMN resource_management.staff_members.user_id IS 'UserId of the FIELD_BRIGADE account of the member in Identity Access, or NULL.';
