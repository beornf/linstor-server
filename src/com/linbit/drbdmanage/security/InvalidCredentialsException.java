package com.linbit.drbdmanage.security;

/**
 * Thrown to indicate an invalid combination of identity name and password
 *
 * @author Robert Altnoeder &lt;robert.altnoeder@linbit.com&gt;
 */
public class InvalidCredentialsException extends SignInException
{
    public InvalidCredentialsException(String message)
    {
        super(message);
    }

    public InvalidCredentialsException(String message, Exception cause)
    {
        super(message, cause);
    }

    public InvalidCredentialsException(Exception cause)
    {
        super(cause);
    }
}
