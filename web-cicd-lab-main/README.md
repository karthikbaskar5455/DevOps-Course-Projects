# Lab 10 — Web CI/CD with Jenkins Freestyle Job, Docker, and a GitHub Webhook

A static page served by `nginx` in Docker, deployed automatically by a
Jenkins **Freestyle** job that's triggered by a GitHub webhook on every push.

## 0. Port conflict warning

Container name `container1` on host port **8081** is what this lab's
instructions hardcode. If you've done other labs on this machine, you may
already have something bound to 8081 (e.g. a `student-management-app`
container from an earlier CI/CD exercise). Free the port first:

```powershell
docker ps
docker stop student-management-app
docker rm student-management-app
```
(skip if nothing's using 8081 — `docker ps` will show you)

## 1. Test the Docker build locally first

```powershell
cd web-cicd-lab
docker build -t nginx-image1 .
docker run -d -p 8081:80 --name=container1 nginx-image1
```
Open `http://localhost:8081` — you should see the "Web CI/CD Lab" page,
badge reading "Version: v1".

```powershell
docker rm --force container1
```
(stop it — Jenkins will run its own copy in Step 4 below)

## 2. Push to GitHub

```powershell
git branch -M main
git remote add origin https://github.com/RawMangoking/web-cicd-lab.git
git push -u origin main
```
(create the repo on GitHub first at github.com/new, same as your earlier labs)

## 3. Create the Jenkins Freestyle job

1. Jenkins dashboard → **New Item**
2. Name it `web-cicd-lab`, select **Freestyle project** → OK
3. Check **"This project is parameterized"**
4. **Add Parameter → String Parameter**
   - Name: `GIT_REPO_URL`
   - Default value: `https://github.com/RawMangoking/web-cicd-lab.git`
5. Scroll to **Source Code Management** → select **Git**
   - Repository URL: `$GIT_REPO_URL` (this references the parameter above)
   - Credentials: select your existing `github-token` credential
   - Branch Specifier: `*/main` (your repo's default branch is `main`, not `master`)
6. Scroll to **Build Triggers** → check **"GitHub hook trigger for GITScm polling"**

## 4. Add the build step

Scroll to **Build** → **Add build step** → **Execute Windows batch command**
(not "Execute shell" — this Jenkins runs on Windows), and paste:

```bat
docker rm --force container1
docker build -t nginx-image1 .
docker run -d -p 8081:80 --name=container1 nginx-image1
```

Click **Save**.

## 5. Expose Jenkins to the internet (needed for the webhook)

GitHub's servers need to reach your Jenkins over the public internet to
deliver the webhook — `localhost` only works on your own machine, so a
plain `http://localhost:9090/github-webhook/` URL is unreachable from
GitHub. The standard fix for a local Jenkins is a tunnel via **ngrok**:

1. Download from `https://ngrok.com/download`, install it
2. Sign up for a free account, copy your authtoken from the ngrok dashboard
3. ```powershell
   ngrok config add-authtoken YOUR_AUTHTOKEN
   ngrok http 9090
   ```
   (use whatever port your Jenkins actually runs on — yours is 9090, not
   the default 8080, since 8080 was already taken during setup)
4. ngrok prints a forwarding URL like `https://random-name.ngrok-free.app`
   — keep this terminal window open the whole time you're testing the
   webhook; closing it kills the tunnel and changes the URL next time.

## 6. Set up the GitHub webhook

1. Go to your repo on GitHub → **Settings** → **Webhooks** → **Add webhook**
2. **Payload URL**: `https://random-name.ngrok-free.app/github-webhook/`
   (your actual ngrok URL from Step 5, with `/github-webhook/` appended —
   trailing slash matters)
3. **Content type**: `application/json`
4. Leave "Just the push event" selected
5. **Add webhook**

GitHub will immediately send a test ping — check the webhook's **Recent
Deliveries** tab; a green checkmark means Jenkins received it.

## 7. Trigger the pipeline

Edit `index.html` — change `Version: v1` to `Version: v2` — then:
```powershell
git add index.html
git commit -m "Bump version to v2 to test webhook trigger"
git push
```

Watch the Jenkins job page — a new build should start within a few seconds
automatically, with no one clicking "Build Now."

## 8. Verify the deployment

```powershell
docker ps
```
Confirm `container1` is `Up`, then open `http://localhost:8081` — the page
should now show **"Version: v2"**, proving Jenkins actually rebuilt and
redeployed the container off the webhook, not off a stale image.
