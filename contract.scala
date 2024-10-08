{
  // Validation of the box replication process
  val isSelfReplication = {
  
    // The input box must have the same block limit as the output box
    val sameBlockLimit = SELF.R5[Int].get == OUTPUTS(0).R5[Int].get

    // The ERG/Token exchange rate must be identical
    val sameExchangeRate = SELF.R6[Long].get == OUTPUTS(0).R6[Long].get

    // The project address must remain the same
    val sameProjectAddress = SELF.R7[GroupElement].get == OUTPUTS(0).R7[GroupElement].get

    // The project link (e.g., GitHub URL) must be unchanged
    val sameProjectLink = SELF.R8[Coll[Byte]].get == OUTPUTS(0).R8[Coll[Byte]].get

    // The script should remain the same
    val sameScript = SELF.propositionBytes == OUTPUTS(0).propositionBytes

    // Verify that the output box is a valid copy of the input box
    sameBlockLimit && sameExchangeRate && sameProjectAddress && sameProjectLink && sameScript
  }

  // Validation for purchasing Tokens
  val isBuyTokens = {
    // Can only be executed if the current height is less than or equal to the block limit.
    val beforeBlockLimit = HEIGHT <= SELF.R5[Int].get

    val userBox = OUTPUTS(1)
    val contractHasTokens = SELF.tokens.nonEmpty
    val userHasTokens = userBox.tokens.nonEmpty && userBox.tokens(0)._1 == SELF.tokens(0)._1
    
    // Calculate the added value from the user's ERG payment
    val addedValueToTheContract = OUTPUTS(0).value - SELF.value

    // Verify if the ERG amount matches the required exchange rate for the given Token quantity
    val correctExchange = addedValueToTheContract == userBox.tokens(0)._2 * SELF.R6[Long].get

    isSelfReplication && beforeBlockLimit && userHasTokens && correctExchange
  }

  // Condition to check if the current height is beyond the block limit
  val afterBlockLimit = HEIGHT > SELF.R5[Int].get

  // Validation for refunding Tokens
  val isRefundTokens = {
    // Refund can only occur if the current height is greater than the block limit and the Tokens are returned.
    val outputUserBox = OUTPUTS(1)
    val inputUserBox = INPUTS(1)
    
    // Check if Tokens are being returned from the user's input box back to the contract
    val returningTokens = inputUserBox.tokens.nonEmpty && SELF.tokens.nonEmpty && inputUserBox.tokens(0)._1 == SELF.tokens(0)._1
    
    // Calculate the value returned from the contract to the user
    val retiredValueFromTheContract = SELF.value - OUTPUTS(0).value

    // Verify if the ERG amount matches the required exchange rate for the returned Token quantity
    val correctExchange = retiredValueFromTheContract == inputUserBox.tokens(0)._2 * SELF.R6[Long].get

    // Ensure all Tokens are refunded
    val refundAllTheTokens = outputUserBox.tokens.isEmpty

    // The contract returns the equivalent ERG value for the returned Tokens
    isSelfReplication && afterBlockLimit && returningTokens && correctExchange && refundAllTheTokens
  }

  // Validation for withdrawing funds
  val isWithdrawFunds = {
    // Funds must be sent to the project's specified address in R7.
    val outputBox = OUTPUTS(0)
    val isToProjectAddress = outputBox.propositionBytes == SELF.R7[GroupElement].get
    
    // Ensure that no Tokens are present in the contract box
    val hasNoTokens = SELF.tokens.isEmpty

    // The value sent to the project's box must equal the total value of the current contract box.
    afterBlockLimit && hasNoTokens && isToProjectAddress && outputBox.value == SELF.value
  }

  // The contract allows box replication, purchasing Tokens, requesting refunds, and withdrawing funds before the block limit.
  isBuyTokens || isRefundTokens || isWithdrawFunds
}
