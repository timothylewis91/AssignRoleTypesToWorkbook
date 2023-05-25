  public String getWorkbookRole(Long workBookId, Long userID) {
        try {
            WorkbookPermissionEntity workbook = workbookPermissionRepository.findByWorkbookIdAndUserId(workBookId, userID);
            return workbook.getRoleType();
        }
        catch(Exception e){
            return "ROLE";
        }
    }
    
         List<WorkbookPermissionEntity> permissions = new ArrayList<>();
        for (AppUserEntity user : users) {
            WorkbookPermissionEntity permission = new WorkbookPermissionEntity();
            permission.setUser(user);
            permission.setWorkbook(workbookEntity);
            permission.setRoleType(roleType);
            permissions.add(permission);
        }

        workbookPermissionRepository.saveAll(permissions);
       
        List<AssignUsersToWorkbookObj> results = new ArrayList<>();
        for (AppUserEntity user : users) {
            AssignUsersToWorkbookObj result = new AssignUsersToWorkbookObj();
            result.setWorkbookId(workbookId);
            result.setUserIds(Collections.singletonList(user.getId()));
            result.setRoleType(roleType);
            results.add(result);
        }
        return results;
    }
