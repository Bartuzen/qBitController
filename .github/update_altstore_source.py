import argparse
import os
import re

import requests


def get_inputs(release_id):
    url = f"https://api.github.com/repos/{os.environ['GITHUB_REPOSITORY']}/releases/{release_id}"
    headers = {
        "Accept": "application/vnd.github+json",
        "Authorization": f"Bearer {os.environ['GITHUB_TOKEN']}",
    }

    response = requests.get(url, headers=headers)
    response.raise_for_status()

    release_data = response.json()
    ipa_file_data = next(asset for asset in release_data["assets"] if asset["name"].endswith(".ipa"))

    version = release_data["tag_name"][1:]
    date = release_data["published_at"]
    download_url = ipa_file_data["browser_download_url"]
    min_os_version = "14.0"
    size = str(ipa_file_data["size"])

    with open("buildSrc/src/main/java/dev/bartuzen/qbitcontroller/Versions.kt", "r") as f:
        build_version = re.search(r"AppVersionCode\s*=\s*(\d+)", f.read()).group(1)

    with open(f"fastlane/metadata/android/en-US/changelogs/{build_version}.txt", "r") as f:
        description = f.read()

    return {
        "version": version,
        "buildVersion": build_version,
        "date": date,
        "localizedDescription": description,
        "downloadURL": download_url,
        "minOSVersion": min_os_version,
        "size": size
    }


def run_repository_dispatch(altstore_repo, event_type, payload):
    url = f"https://api.github.com/repos/{altstore_repo}/dispatches"
    headers = {
        "Accept": "application/vnd.github+json",
        "Authorization": f"Bearer {os.environ['ALTSTORE_GITHUB_TOKEN']}",
    }

    data = {
        "event_type": event_type,
        "client_payload": payload
    }

    response = requests.post(url, json=data, headers=headers)
    response.raise_for_status()


def main():
    parser = argparse.ArgumentParser(description="Trigger AltStore update via repository_dispatch")

    parser.add_argument("--altstore-repo")
    parser.add_argument("--release-id")
    parser.add_argument("--event-type", default="update-altstore")

    args = parser.parse_args()

    payload = get_inputs(args.release_id)
    run_repository_dispatch(args.altstore_repo, args.event_type, payload)


if __name__ == "__main__":
    main()
