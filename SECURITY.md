# Security Policy

## Reporting a vulnerability

Please report security issues **privately** — do not open a public issue.

- Preferred: GitHub → **Security** tab → **Report a vulnerability** (private
  advisory) on [pierrejochem/Speculum](https://github.com/pierrejochem/Speculum/security/advisories/new).
- Or email the maintainer: **pierrejochem@msn.com**.

Please include affected version, reproduction steps, and impact. Expect an
initial response within a few days.

## Supported versions

Speculum is pre-1.0; only the **latest release** receives fixes. Upgrade to the
newest tag before reporting.

## Verifying releases

Every release ships a `SHA256SUMS` file covering all packages
(`.deb` / `.rpm` / `.pkg.tar.*`):

```bash
sha256sum -c SHA256SUMS --ignore-missing
```

When release signing is configured, each asset and `SHA256SUMS` also gets a
detached `.asc` GPG signature.

### Public GPG key

The signing key is published in [`KEYS`](KEYS) in this repository. Import and
verify:

```bash
# import the maintainer's public key
curl -fsSL https://raw.githubusercontent.com/pierrejochem/Speculum/main/KEYS | gpg --import

# verify the checksums file, then an asset
gpg --verify SHA256SUMS.asc SHA256SUMS
gpg --verify speculum_<version>_arm64.deb.asc speculum_<version>_arm64.deb
```

Key fingerprint (verify the imported key matches):

```
4F73521A2DB000391BEF252AAD96D4494AF02FA1
```

> Maintainer setup: export the **public** key to `KEYS`
> (`gpg --armor --export <KEY_ID> > KEYS`), commit it, and replace the
> fingerprint above (`gpg --fingerprint <KEY_ID>`). The matching **private**
> key + passphrase go in the repo secrets `GPG_PRIVATE_KEY` / `GPG_PASSPHRASE`
> (see [PACKAGING.md](PACKAGING.md) → *Verifying downloads*).