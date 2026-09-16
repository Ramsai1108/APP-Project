package com.campus.emergency.model;

/**
 * Role entity representing user access levels.
 */
public class Role extends BaseEntity {
    private String roleName;
    private String description;

    public Role() {}

    public Role(Long id, String roleName, String description) {
        super(id);
        this.roleName = roleName;
        this.description = description;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return roleName;
    }
}
