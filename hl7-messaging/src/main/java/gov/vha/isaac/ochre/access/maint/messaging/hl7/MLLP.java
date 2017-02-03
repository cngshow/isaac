/**
 * Copyright Notice
 *
 * This is a work of the U.S. Government and is not subject to copyright
 * protection in the United States. Foreign copyrights may apply.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gov.vha.isaac.ochre.access.maint.messaging.hl7;

/**
 * The Minimal Lower Layer protocol (MLLP). This protocol is a minimalistic
 * OSI-session layer framing protocol used by BusinessWare when sending HL7
 * messages.
 * <p>
 * HL7 content is enclosed by special characters to form a block. The block
 * format is as follows: &lt;SB&gt;dddd&lt;EB&gt;&lt;CR&gt;
 * <p>
 * <table>
 * <tr>
 * <th>Element Description</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>&lt;SB&gt</td>
 * <td>Start Block character (1 byte). ASCII &lt;VT&gt; , i.e., 0x0B. This
 * should not be confused with the ASCII characters SOH or STX.</td>
 * </tr>
 * <tr>
 * <td>dddd</td>
 * <td>Data (variable number of bytes). This is the HL7 data content of the
 * block. The data can contain any single-byte values greater than 0x1F (see
 * next paragraph for issues related to character encodings) and the ASCII
 * carriage return character, &lt;CR&gt;.</td>
 * </tr>
 * <tr>
 * <td>&lt;EB&gt;</td>
 * <td>End Block character (1 byte). ASCII &lt;FS&gt;, i.e., 0x1C. This should
 * not be confused with the ASCII characters ETX or EOT.</td>
 * </tr>
 * <tr>
 * <td>&lt;CR&gt;</td>
 * <td>Carriage Return (1 byte). The ASCII carriage return character, i.e.,
 * 0x0D.</td>
 * </tr>
 * </table>
 * <p>
 * The MLLP block is framed by single-byte values. The characters transmitted
 * within the MLLP block have to be encoded according to a character encoding
 * that does not conflict with the byte values used for framing. Some multi-byte
 * character encodings (e.g. UTF-16, UTF-32) may result in byte values equal to
 * the MLLP framing characters or byte values lower than 0x1F, resulting in
 * errors. These character encodings are therefore not supported by MLLP.
 * <p>
 * MLLP supports all single-byte character encodings (e.g. iso-8859-x, cp1252)
 * as well as UTF-8 and Shift_JIS.
 */
public class MLLP
{
	public static final char SB = 0x0B;
	public static final char EB = 0x1C;
	public static final char CR = 0x0D;
}
