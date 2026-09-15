#!/usr/bin/env groovy

@Library('ghafInfra') _

def DEFAULT_REPO_URL = 'https://github.com/tiiuae/ghaf-humanoids/'
def PIPELINE = [:]

properties([
  githubProjectProperty(displayName: ''),
  parameters([
    booleanParam(name: 'UEFISIGN', defaultValue: false, description: 'Enable secure boot signing (for supported targets)'),
    booleanParam(name: 'SECUREBOOT', defaultValue: false, description: 'Run tests also on secureboot enabled hardware, if available'),
    string(name: 'REPO_URL', defaultValue: DEFAULT_REPO_URL, description: 'Git repository URL'),
    string(name: 'GITREF', defaultValue: 'main', description: 'Ghaf git reference (Commit/Branch/Tag)'),
    string(name: 'CREDENTIALS_ID', defaultValue: 'github-ghaf-humanoids', description: 'Jenkins credentialsId for cloning a private repository; leave empty for anonymous checkout'),
    string(name: 'TESTSET', defaultValue: null, description: 'By default tests are skipped. To run hw-tests, define the target testset here; e.g.: _relayboot_, _relayboot_bat_, _relayboot_pre-merge_, etc.)'),
    booleanParam(name: 'nvidia_jetson_orin_agx_debug_from_x86_64', defaultValue: false, description: 'Build target packages.x86_64-linux.nvidia-jetson-orin-agx-debug-from-x86_64'),
    booleanParam(name: 'nvidia_jetson_orin_agx_debug', defaultValue: false, description: 'Build target packages.aarch64-linux.nvidia-jetson-orin-agx-debug'),
    booleanParam(name: 'nvidia_jetson_orin_nx_debug', defaultValue: false, description: 'Build target packages.aarch64-linux.nvidia-jetson-orin-nx-debug'),
    booleanParam(name: 'delivery_orinnx_lab', defaultValue: true, description: 'Build target packages.aarch64-linux.delivery-orinnx-lab'),
    booleanParam(name: 'delivery_orinnx_ark_bench', defaultValue: false, description: 'Build target packages.aarch64-linux.delivery-orinnx-ark-bench'),
 ])
])
pipeline {
  agent none
  options {
    buildDiscarder(logRotator(numToKeepStr: '30'))
  }
  stages {
    stage('Reload only') {
      agent { label 'built-in' }
      when { expression { params && params.RELOAD_ONLY } }
      steps {
        script {
          currentBuild.result = 'ABORTED'
          currentBuild.displayName = "Reloaded pipeline"
          error('Reloading pipeline - aborting other stages')
        }
      }
    }
    stage('Checkout') {
      agent { label 'built-in' }
      steps {
        dir(artifactSupport.controller_workdir()) {
          script {
            if (params.CREDENTIALS_ID?.trim()) {
              deleteDir()
              checkout scmGit(
                branches: [[name: params.GITREF]],
                userRemoteConfigs: [[
                  url: params.REPO_URL,
                  credentialsId: params.CREDENTIALS_ID,
                ]],
                extensions: [[$class: 'CloneOption', noTags: true, timeout: 30]],
              )
            } else {
              checkoutUtils.checkout_remote_ref(params.REPO_URL, params.GITREF)
            }
          }
        }
      }
    }
    stage('Setup') {
      agent { label 'built-in' }
      steps {
        dir(artifactSupport.controller_workdir()) {
          script {
            def TARGETS = []
            if (params.nvidia_jetson_orin_agx_debug_from_x86_64) {
              TARGETS.push(
                [ target: "packages.x86_64-linux.nvidia-jetson-orin-agx-debug-from-x86_64", uefisign: params.UEFISIGN, testset: params.TESTSET ])
            }
            if (params.nvidia_jetson_orin_agx_debug) {
              TARGETS.push(
                [ target: "packages.aarch64-linux.nvidia-jetson-orin-agx-debug", uefisign: params.UEFISIGN, testset: params.TESTSET ])
            }
            if (params.nvidia_jetson_orin_nx_debug) {
              TARGETS.push(
                [ target: "packages.aarch64-linux.nvidia-jetson-orin-nx-debug", uefisign: params.UEFISIGN, testset: params.TESTSET ])
            }
            if (params.delivery_orinnx_lab) {
              TARGETS.push(
                [ target: "packages.aarch64-linux.delivery-orinnx-lab", uefisign: params.UEFISIGN, testset: params.TESTSET ])
            }
            if (params.delivery_orinnx_ark_bench) {
              TARGETS.push(
                [ target: "packages.aarch64-linux.delivery-orinnx-ark-bench", uefisign: params.UEFISIGN, testset: params.TESTSET ])
            }

            PIPELINE = pipelineExecution.create_pipeline(TARGETS)
          }
        }
      }
    }
    stage('Build') {
      steps {
        script {
          parallel PIPELINE
        }
      }
    }
  }
  post {
    always {
      script {
        artifactSupport.clean_controller_workdir()
      }
    }
  }
}
