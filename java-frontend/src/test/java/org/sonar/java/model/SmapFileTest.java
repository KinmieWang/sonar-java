/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class SmapFileTest {

  @Test
  public void test() {
    String sourceMap = "SMAP\n" +
      "test_jsp.java\n" +
      "JSP\n" +
      "*S JSP\n" +
      "*F\n" +
      "+ 0 test.jsp\n" +
      "WEB-INF/test.jsp\n" +
      "*L\n" +
      "1,5:116,0\n" +
      "123:207\n" +
      "130,3:210\n" +
      "140:250,7\n" +
      "160,3:300,2\n" +
      "160#2,3:300,2\n" +
      "160,3:300,2\n" +
      "*E\n";
    Path sourceMapPath = Paths.get("/some/path/test_jsp.class.smap");
    SmapFile smap = new SmapFile(sourceMapPath, new Scanner(sourceMap));
    assertThat(smap.getGeneratedFile()).isEqualTo(Paths.get("/some/path/test_jsp.java"));
    assertThat(smap.getFileSection()).containsExactly(entry(0, new SmapFile.FileInfo(0, "test.jsp", "WEB-INF/test.jsp")));
    assertThat(smap.getLineSection()).containsExactly(
      new SmapFile.LineInfo(1, 0, 5, 116, 0),
      new SmapFile.LineInfo(123, 0, 1, 207, 1),
      new SmapFile.LineInfo(130, 0, 3, 210, 1),
      new SmapFile.LineInfo(140, 0, 1, 250, 7),
      new SmapFile.LineInfo(160, 0, 3, 300, 2),
      new SmapFile.LineInfo(160, 2, 3, 300, 2),
      new SmapFile.LineInfo(160, 2, 3, 300, 2)
    );
  }

}
