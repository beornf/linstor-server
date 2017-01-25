package com.linbit.drbdmanage;

import com.linbit.ValueOutOfRangeException;

/**
 * Unix minor number
 *
 * @author raltnoeder
 */
public class MinorNumber implements Comparable<MinorNumber>
{
    public static final int MINOR_NR_MIN = 0;
    public static final int MINOR_NR_MAX = (1 << 20) - 1;

    private static final String MINOR_NR_EXC_FORMAT =
        "Minor number %d is out of range [%d - %d]";

    public final int value;

    public MinorNumber(int number) throws ValueOutOfRangeException
    {
        minorNrCheck(number);
        value = number;
    }

    @Override
    public int compareTo(MinorNumber other)
    {
        int result = 0;
        if (other == null)
        {
            // null sorts before any existing VolumeNumber
            result = 1;
        }
        else
        {
            if (this.value < other.value)
            {
                result = -1;
            }
            else
            if (this.value > other.value)
            {
                result = 1;
            }
        }
        return result;
    }

    public boolean equals(MinorNumber other)
    {
        return other != null && other.value == this.value;
    }

    @Override
    public int hashCode()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return Integer.toString(value);
    }

    /**
     * Checks the validity of a UNIX minor number
     *
     * @param minorNr The minor number to check
     * @throws ValueOutOfRangeException If the minor number is out of range
     */
    public static void minorNrCheck(int minorNr) throws ValueOutOfRangeException
    {
        Checks.genericRangeCheck(minorNr, MINOR_NR_MIN, MINOR_NR_MAX, MINOR_NR_EXC_FORMAT);
    }
}
