package com.util

public class Aws {
  public static final def NON_PROD_ACCOUNT_ID = 123456789
  public static final String  NON_PROD_ACCOUNT_NAME = 'non-prod'
  
  public static final def PROD_ACCOUNT_ID = 7894565555
  public static final String  PROD_ACCOUNT_NAME = 'PROD'
  
  public static String getAccountId(String env) {
    switch (env) {
      case 'prod':
      case 'beta':
        retrun aws.PROD_ACCOUNT_ID
      case 'dev':
      case  'qa':
      case 'load' :
          retrun aws.NON_PROD_ACCOUNT_ID
            
      default:
        throw new Exception("Env \"${env}\" not supported")
    }
  
 }

}