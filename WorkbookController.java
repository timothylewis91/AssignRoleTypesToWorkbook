//Check if requester is owner and workbook is not locked
        String response = (workbookService.getWorkbookRole(workbookID, user.getId()));
        if (response.equals("OWNER") && getWorkbookResponseObj.get().getLock() == null ){
            Optional<WorkbookResponseObj> workbookObj = workbookService.deleteWorkbook(workbookID);
            workbookObj.get().setMessage("Workbook was deleted.");
            return ResponseEntity.status(HttpStatus.OK).body(workbookObj.get());
        }

        WorkbookResponseObj invalidUser = new WorkbookResponseObj();
        invalidUser.setId(getWorkbookResponseObj.get().getId());
        invalidUser.setMessage("Not authorized to delete workbook");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(invalidUser);
    }

    @PutMapping("/openWorkbook/{workbookId}")
    @PreAuthorize("hasAuthority('RETRIEVE_WORKBOOK_DATA')")
    @Operation(description = "open a workbook",
            responses = @ApiResponse(content = @Content(
                    array = @ArraySchema(schema = @Schema(implementation = OpenWorkbookResponseObj.class)))))
    public ResponseEntity<OpenWorkbookResponseObj> openWorkbook(@PathVariable("workbookId") final Long workbookId) {

        // Declare and instantiate the user and workbook response from the database
        AppUserObj user = (AppUserObj) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Check if workbook exist
        Optional<GetWorkbookResponseObj> workbook = workbookService.getWorkbook(workbookId);
        if (workbook.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        OpenWorkbookResponseObj response = workbookService.getOpenWorkbook(workbookId, user.getId());

        // Check if user is owner or collaborator to open workbook
        if (response !=null && (response.getRoleType().equals("OWNER") || response.getRoleType().equals("COLLABORATOR"))){
            workbookService.unlockWorkbooks(user.getId());
            if(response.getLock() == null || response.getLock() == user.getId()) {
                workbookService.lockWorkbook(user.getId(), workbookId);
                response.setLock(user.getId());
            }
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }
        else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PutMapping("/assignUsersToWorkbooks")
    @PreAuthorize("hasAuthority('ASSIGN_WORKBOOK_PERMISSION')")
    @Operation(description = "Assign owners/collaborators to a workbook", responses = @ApiResponse(content = @Content(schema = @Schema(implementation = WorkbookResponseObj.class))))
    public ResponseEntity<WorkbookResponseObj> assignUsersToWorkbook(
            @RequestBody final AssignUsersToWorkbookRequestObj request) throws Exception {

        AssignUsersToWorkbookObj command = request.toCommand();
        List<AssignUsersToWorkbookObj> result;

        HttpStatus status;
        String message;


        AppUserObj user = (AppUserObj) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long workbookId = request.getWorkbookId();
        String role = (workbookService.getWorkbookRole(workbookId, user.getId()));
        if(role.equals("OWNER") || user.getRoles().stream().map(r->r.getName()).toList().contains("ADMIN")){

            try {
                result = workbookService.assignUsersToWorkbook(command);

                // Set the appropriate HTTP status / message based on the returned results
                status = result.isEmpty() && !command.getUserIds().isEmpty() ? HttpStatus.NOT_FOUND : HttpStatus.OK;
                message = result.isEmpty() && !command.getUserIds().isEmpty() ?"No user found" : "Assign users to workbook successful";

            } catch (EntityNotFoundException e) {
                status = HttpStatus.BAD_REQUEST;
                message = "Exception occurred, unable to assign users to workbook";
            }
        }
        else {
            status = HttpStatus.UNAUTHORIZED;
            message = "Unauthorize to assign users to workbook";
        }
        WorkbookResponseObj response = new WorkbookResponseObj(workbookId, message);

        return ResponseEntity.status(status).body(response);
    }
