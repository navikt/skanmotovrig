#!/usr/bin/env sh

#PGP
if test -f /var/run/secrets/nais.io/skanmotovrig/pgpprivatekey; then
  echo "Setting skanmotovrig_pgp_privateKey"
  export skanmotovrig_pgp_privateKey=/var/run/secrets/nais.io/skanmotovrig/pgpprivatekey
fi

#SFTP
if test -f /var/run/secrets/nais.io/skanmotovrig/privatekey; then
  echo "Setting skanmotovrig_sftp_privateKey"
  export skanmotovrig_sftp_privateKey=/var/run/secrets/nais.io/skanmotovrig/privatekey
fi

if test -f /var/run/secrets/nais.io/skanmotovrig/hostkey; then
  echo "Setting skanmotovrig_sftp_hostKey"
  export skanmotovrig_sftp_hostKey=/var/run/secrets/nais.io/skanmotovrig/hostkey
fi