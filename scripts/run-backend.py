#!/usr/bin/env python3
"""Local IDE alternative: load generated configuration and run Spring Boot on the host."""
from pathlib import Path
import os, shutil, subprocess

def main():
    root=Path(__file__).resolve().parents[1]; path=root/".env"
    if not path.is_file():raise SystemExit("Run python scripts/setup.py first.")
    values={}
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.lstrip().startswith("#"):continue
        key,sep,value=line.partition("=")
        if not sep:raise SystemExit("Invalid .env entry")
        values[key.strip()]=value.strip()
    if values.get("PRODUCTION","false").lower()=="true":raise SystemExit("Refusing to use production configuration for local development.")
    command=shutil.which("mvn.cmd" if os.name=="nt" else "mvn")
    if not command:raise SystemExit("Install Maven 3.10.0 and Java 27 on PATH, or use Docker Compose.")
    env=os.environ.copy();env.update(values)
    env.update(DATABASE_URL="jdbc:postgresql://127.0.0.1:5432/pruefpilot",DATABASE_USER="pruefpilot_app",MIGRATION_USER="pruefpilot_owner")
    raise SystemExit(subprocess.call([command,"-f",str(root/"backend/pom.xml"),"spring-boot:run"],cwd=root,env=env))
if __name__=="__main__":main()
