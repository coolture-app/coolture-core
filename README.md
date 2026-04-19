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