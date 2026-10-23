#!/usr/bin/env python3
"""Generate local secrets, never overwrite an existing configuration. Python standard library only."""
from pathlib import Path
import argparse, os, secrets

def main():
    parser=argparse.ArgumentParser(description="Create PruefPilot local configuration")
    parser.add_argument("--demo",action="store_true",help="Seed fictional backend demo data on first start")
    parser.add_argument("--email",default="owner@example.test",help="Initial administrator login")
    args=parser.parse_args()
    if "@" not in args.email or any(c in args.email for c in "\r\n'\"$ "):
        parser.error("Use a simple valid email address without whitespace")
    root=Path(__file__).resolve().parents[1]; path=root/".env"
    if path.exists():
        raise SystemExit(".env already exists; left unchanged. Edit deliberately; never reset a live database to change credentials.")
    values={"POSTGRES_ADMIN_PASSWORD":secrets.token_urlsafe(32),"MIGRATION_PASSWORD":secrets.token_urlsafe(32),"DATABASE_PASSWORD":secrets.token_urlsafe(32),"ADMIN_PASSWORD":secrets.token_urlsafe(24),"ADMIN_EMAIL":args.email,"ORGANIZATION_NAME":"PruefPilot Workspace","ORGANIZATION_TIMEZONE":"Europe/Berlin","PUBLIC_URL":"http://localhost:8080","APP_PORT":"8080","COOKIE_SECURE":"false","PRODUCTION":"false","DEMO_DATA":str(args.demo).lower(),"DEMO_PASSWORD":secrets.token_urlsafe(24) if args.demo else ""}
    descriptor=os.open(path,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
    with os.fdopen(descriptor,"w",encoding="utf-8") as out:
        out.write("# LOCAL SECRETS. Never commit, publish, attach to issues or include in backups without encryption.\n")
        out.write("".join(f"{key}={value}\n" for key,value in values.items()))
    print("Created .env. Read ADMIN_EMAIL and ADMIN_PASSWORD there; secrets are not printed to console.")
    print("Next: docker compose up --build -d")
    print("Open http://localhost:8080 . For public deployment, complete docs/en/DEPLOYMENT.md.")
    if os.name=="nt":print("Windows: restrict the .env file's ACL to your account.")
if __name__=="__main__":main()
