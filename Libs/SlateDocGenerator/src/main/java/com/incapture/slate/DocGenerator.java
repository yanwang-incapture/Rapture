/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2011-2016 Incapture Technologies LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.incapture.slate;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.google.common.base.Optional;
import com.incapture.slate.model.node.ApiNode;
import com.incapture.slate.model.node.FileNode;
import com.incapture.slate.model.node.IndexNode;

/**
 * @author bardhi
 * @since 6/2/15.
 */
public class DocGenerator {
    public void generateOutput(File rootDir, IndexNode indexNode, List<ApiNode> nodes) throws IOException {
        for (FileNode node : nodes) {
            indexNode.getSectionsNames().add(node.getTrimmedTitle());
            writeFile(rootDir, node);
        }
        writeFile(rootDir, indexNode);
    }

    private void writeFile(File rootDir, FileNode node) throws IOException {
        Optional<String> dirName = node.getDirName();
        File dirToWrite;
        if (dirName.isPresent()) {
            dirToWrite = new File(rootDir, dirName.get());
        } else {
            dirToWrite = rootDir;
        }
        File file = new File(dirToWrite, node.getFilename());
        String content = node.getContent();
        FileUtils.write(file, content);
    }
}
