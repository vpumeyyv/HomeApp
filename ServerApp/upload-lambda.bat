@echo off

set lambda=HomeApp
set zipfile=ServerApp.zip
set handler=lambda-datastore.lambda_handler
set role=arn:aws:iam::088169412392:role/service-role/Lambda_run
rem set region=eu-west-1
set region=eu-west-1

set action=%1

if /I "%action%"=="create" (
		aws lambda create-function   ^
		 --function-name %lambda%    ^
		   --runtime "python2.7"     ^
		   --role %role%             ^
		   --handler %handler%       ^
		   --description "HomeApp database API"  ^
		   --publish                 ^
		   --zip-file fileb://%zipfile%  ^
		   --region %region%  ^
		   --timeout 5 ^
		   --output json
		goto continue
		)
if /I "%action%"=="update" (	
		aws lambda update-function-code   ^
		  --function-name %lambda%   ^
			--publish                 ^
			--zip-file fileb://%zipfile% ^
			--region %region% ^
			--output json
		goto continue 
	 echo %action% Not Supported.
)
echo Usage: %0 {create^|update}
exit /B
 
:continue


:end