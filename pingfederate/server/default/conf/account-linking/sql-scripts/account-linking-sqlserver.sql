/*------------------------------------------------------------
 * Create table pingfederate_account_link
 */-----------------------------------------------------------

CREATE TABLE pingfederate_account_link(
    idp_entityid    NVARCHAR(162),
    external_userid NVARCHAR(256),
    adapter_id      NVARCHAR(32),
    local_userid    NVARCHAR(256),
    date_created    DATETIME NOT NULL,
    CONSTRAINT pk_account PRIMARY KEY (idp_entityid, external_userid, adapter_id)
);
CREATE INDEX LOCALUSERIDIDX ON pingfederate_account_link(local_userid);

GO
