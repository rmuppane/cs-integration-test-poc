Feature: Test case 'cs'

    Scenario: 1
    		Given a request to check for 'test-dmn.EvaluatedDesignOption1B' when the customer financial status is 
	      |state   										|uk   				|
	      |zone												|London				|
	      |dateOfIncorporationMonths	|6						|
	      |dateOfIncorporation				|2021-01-01		|
	      |companyTypeEnName					|Ltd					|
	      |countryCode								|165					|
	      And customer Loan might be 'Approved'
	      |documetEnName | rama.doc |
	      |documetFrName | rama.doc |
	      |documetEnName | rama.doc |