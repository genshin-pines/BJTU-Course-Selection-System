# BJTU Course Review Deployment

Target server:

- OS: Ubuntu 22.04 LTS
- Public IP: 101.32.222.213
- Web: Nginx on port 80
- Backend: Spring Boot on local port 8889
- Database: MySQL local database `bjtu_review`

## 1. Upload project

Easiest way:

```powershell
cd C:\Users\34653\Desktop\bjtu\BJTU-Course-Selection-System
powershell -ExecutionPolicy Bypass -File .\deploy\upload-and-install.ps1 -User ubuntu
```

This uploads the project and starts setup on the server. Enter your server password when PowerShell asks for it.

Manual upload:

Run this on your Windows PowerShell from the project parent folder:

```powershell
cd C:\Users\34653\Desktop\bjtu
scp -r .\BJTU-Course-Selection-System ubuntu@101.32.222.213:~/
```

If Tencent Cloud gave you a different login username, replace `ubuntu`.

## 2. Login server

```powershell
ssh ubuntu@101.32.222.213
```

## 3. Run setup

Run this on the server:

```bash
cd ~/BJTU-Course-Selection-System
sudo bash deploy/setup-ubuntu.sh
```

The script will ask you for:

- MySQL password for app user `bjtu_review`
- JWT secret, at least 32 characters

Do not use `123456`.

## 4. Open the site

After setup finishes, open:

```text
http://101.32.222.213
```

## Useful commands

Check backend status:

```bash
sudo systemctl status bjtu-review
```

View backend logs:

```bash
sudo journalctl -u bjtu-review -f
```

Restart backend:

```bash
sudo systemctl restart bjtu-review
```

Reload Nginx:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

Rebuild after code changes:

```bash
cd ~/BJTU-Course-Selection-System
sudo bash deploy/setup-ubuntu.sh
```
