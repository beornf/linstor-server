package com.linbit.linstor.storage.layer.provider.openflex.rest.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenflexPool
{
    public String Self;
    public String ID;
    public OpenflexStatus Status;
    public long RemainingCapacity;
    public long TotalCapacity;
    public String UUID;
}
