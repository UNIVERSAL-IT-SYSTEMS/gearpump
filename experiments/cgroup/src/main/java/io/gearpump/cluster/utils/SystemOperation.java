/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gearpump.cluster.utils;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SystemOperation {

    public static final Logger LOG = LoggerFactory.getLogger(SystemOperation.class);

    public static void mount(String name, String target, String type, String data) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("mount -t ").append(type).append(" -o ").append(data).append(" ").append(name).append(" ").append(target);
        SystemOperation.exec(sb.toString());
    }

    public static void umount(String name) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("umount ").append(name);
        SystemOperation.exec(sb.toString());
    }

    public static String exec(String cmd) throws IOException {
        LOG.debug("Shell cmd: " + cmd);
        Process process = new ProcessBuilder(new String[] { "/bin/bash", "-c", cmd }).start();
        try {
            process.waitFor();
            String output = IOUtils.toString(process.getInputStream());
            String errorOutput = IOUtils.toString(process.getErrorStream());
            LOG.debug("Shell Output: " + output);
            if (errorOutput.length() != 0) {
                LOG.error("Shell Error Output: " + errorOutput);
                throw new IOException(errorOutput);
            }
            return output;
        } catch (InterruptedException ie) {
            throw new IOException(ie.toString());
        }

    }

    public static void main(String[] args) throws IOException {
        SystemOperation.mount("test", "/cgroup/cpu", "cgroup", "cpu");
    }

}
