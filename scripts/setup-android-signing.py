#!/usr/bin/env python3
"""Create a local upload key once. Never print or pass passwords on the command line."""
import os
from pathlib import Path
import secrets
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]


def main():
    properties = ROOT / "keystore.properties"
    directory = ROOT / "keystores"
    keystore = directory / "never-sleep-upload.keystore"
    certificate = directory / "never-sleep-upload.pem"
    if properties.exists() or keystore.exists():
        raise SystemExit("Signing already exists. Refusing to replace an upload key or its configuration.")
    java_home = os.environ.get("JAVA_HOME")
    keytool = str(Path(java_home) / "bin/keytool") if java_home else shutil.which("keytool")
    if not keytool:
        raise SystemExit("Install a JDK and put keytool on PATH.")
    directory.mkdir(mode=0o700, exist_ok=True)
    os.chmod(directory, 0o700)
    password = secrets.token_urlsafe(48)
    environment = dict(os.environ, NEVER_SLEEP_KEY_PASSWORD=password)
    with tempfile.TemporaryDirectory(dir=directory) as staging:
        staged = Path(staging) / keystore.name
        subprocess.run(
            [keytool, "-genkeypair", "-noprompt", "-keystore", str(staged),
             "-storetype", "PKCS12", "-storepass:env", "NEVER_SLEEP_KEY_PASSWORD",
             "-keypass:env", "NEVER_SLEEP_KEY_PASSWORD", "-alias", "never-sleep-upload",
             "-keyalg", "RSA", "-keysize", "4096", "-validity", "10000",
             "-dname", "CN=Lizz Studio, OU=Never Sleep"],
            env=environment, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE,
        )
        os.chmod(staged, 0o600)
        staged.rename(keystore)
    # Exclusive creation ensures an existing configuration is never overwritten.
    fd = os.open(properties, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(fd, "w") as output:
        output.write(
            f"storeFile=keystores/{keystore.name}\nstorePassword={password}\n"
            f"keyAlias=never-sleep-upload\nkeyPassword={password}\n"
        )
    subprocess.run(
        [keytool, "-exportcert", "-rfc", "-keystore", str(keystore),
         "-storepass:env", "NEVER_SLEEP_KEY_PASSWORD", "-alias", "never-sleep-upload",
         "-file", str(certificate)],
        env=environment, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE,
    )
    print("Created private upload key and keystore.properties (permissions 600).")
    print("Public upload certificate: keystores/never-sleep-upload.pem")
    print("Back up the keystore and keystore.properties together in secure storage.")


if __name__ == "__main__":
    main()
