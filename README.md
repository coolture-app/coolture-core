# coolture

### Relational Database Design
In `./db/` there is `schema.dbml` file that is version controlled. It is universal file format for storing information about database model. It has a role of an documentation and source of truth for database design. 

Additionally ERD and whole db spec can be generated from this file using `dbdocs` tool. If you want to make changes to the ERD firstly install it: 
`npm install -g dbdocs`

Then use:  
`dbdocs login` and provide credentials in browser which admin will provide on question.  

When changes to `.dbml` file will be made they can be published with:  
`dbdocs <path-to-dbml-file> --project coolture --public --versionName=<changes-made>`

[dbdocs documentation](https://docs.dbdocs.io/)  
[dmbl documentation](https://dbml.dbdiagram.io/home/)

### REST API Contract
In `./api/` there is `contract.yml` file that is version controlled source of truth for API. It contains OpenAPI 3.1 spec describing REST API contract between front and back service to enable parallel development of these and to avoid miscommunications.

Can edit it and preview it easily using Swagger Editor in browser or with `42crunch.vscode-openapi` extension to VS Code.

After full implementation this contract can be abandoned, bcs Spring will provide it's own OpenAPI docs.