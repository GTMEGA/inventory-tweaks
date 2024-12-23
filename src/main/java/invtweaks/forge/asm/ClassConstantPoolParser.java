/***
 * This Class is derived from the ASM ClassReader
 * <p>
 * ASM: a very small and fast Java bytecode manipulation framework Copyright (c) 2000-2011 INRIA, France Telecom All
 * rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met: 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer. 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or other materials provided with the
 * distribution. 3. Neither the name of the copyright holders nor the names of its contributors may be used to endorse
 * or promote products derived from this software without specific prior written permission.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package invtweaks.forge.asm;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.objectweb.asm.Opcodes;

/**
 * Using this class to search for a (single) String reference is > 40 times faster than parsing a class with a
 * ClassReader + ClassNode while using way less RAM
 */
public class ClassConstantPoolParser {

    /** The tag value of CONSTANT_Class_info JVMS structures. */
    private static final int CONSTANT_CLASS_TAG = 7;

    /** The tag value of CONSTANT_Fieldref_info JVMS structures. */
    private static final int CONSTANT_FIELDREF_TAG = 9;

    /** The tag value of CONSTANT_Methodref_info JVMS structures. */
    private static final int CONSTANT_METHODREF_TAG = 10;

    /** The tag value of CONSTANT_InterfaceMethodref_info JVMS structures. */
    private static final int CONSTANT_INTERFACE_METHODREF_TAG = 11;

    /** The tag value of CONSTANT_String_info JVMS structures. */
    private static final int CONSTANT_STRING_TAG = 8;

    /** The tag value of CONSTANT_Integer_info JVMS structures. */
    private static final int CONSTANT_INTEGER_TAG = 3;

    /** The tag value of CONSTANT_Float_info JVMS structures. */
    private static final int CONSTANT_FLOAT_TAG = 4;

    /** The tag value of CONSTANT_Long_info JVMS structures. */
    private static final int CONSTANT_LONG_TAG = 5;

    /** The tag value of CONSTANT_Double_info JVMS structures. */
    private static final int CONSTANT_DOUBLE_TAG = 6;

    /** The tag value of CONSTANT_NameAndType_info JVMS structures. */
    private static final int CONSTANT_NAME_AND_TYPE_TAG = 12;

    /** The tag value of CONSTANT_Utf8_info JVMS structures. */
    private static final int CONSTANT_UTF8_TAG = 1;

    /** The tag value of CONSTANT_MethodHandle_info JVMS structures. */
    private static final int CONSTANT_METHOD_HANDLE_TAG = 15;

    /** The tag value of CONSTANT_MethodType_info JVMS structures. */
    private static final int CONSTANT_METHOD_TYPE_TAG = 16;

    /** The tag value of CONSTANT_Dynamic_info JVMS structures. */
    private static final int CONSTANT_DYNAMIC_TAG = 17;

    /** The tag value of CONSTANT_InvokeDynamic_info JVMS structures. */
    private static final int CONSTANT_INVOKE_DYNAMIC_TAG = 18;

    /** The tag value of CONSTANT_Module_info JVMS structures. */
    private static final int CONSTANT_MODULE_TAG = 19;

    /** The tag value of CONSTANT_Package_info JVMS structures. */
    private static final int CONSTANT_PACKAGE_TAG = 20;

    private byte[][] BYTES_TO_SEARCH;

    public ClassConstantPoolParser(String... strings) {
        BYTES_TO_SEARCH = new byte[strings.length][];
        for (int i = 0; i < BYTES_TO_SEARCH.length; i++) {
            BYTES_TO_SEARCH[i] = strings[i].getBytes(StandardCharsets.UTF_8);
        }
    }

    public void addString(String string) {
        BYTES_TO_SEARCH = Arrays.copyOf(BYTES_TO_SEARCH, BYTES_TO_SEARCH.length + 1);
        BYTES_TO_SEARCH[BYTES_TO_SEARCH.length - 1] = string.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Returns true if the constant pool of the class represented by this byte array contains one of the Strings we are
     * looking for
     */
    public boolean find(byte[] basicClass) {
        return find(basicClass, false);
    }

    private static final int V24 = 68;

    /**
     * Returns true if the constant pool of the class represented by this byte array contains one of the Strings we are
     * looking for.
     *
     * @param prefixes If true, it is enough for a constant pool entry to <i>start</i> with one of our Strings to count
     *                 as a match - otherwise, the entire String has to match.
     */
    public boolean find(byte[] basicClass, boolean prefixes) {
        if (basicClass == null || basicClass.length == 0) {
            return false;
        }
        // checks the class version
        if (readShort(6, basicClass) > V24) {
            return false;
        }
        // parses the constant pool
        final int n = readUnsignedShort(8, basicClass);
        int index = 10;
        for (int i = 1; i < n; ++i) {
            final int size;
            switch (basicClass[index]) {
                case CONSTANT_FIELDREF_TAG:
                case CONSTANT_METHODREF_TAG:
                case CONSTANT_INTERFACE_METHODREF_TAG:
                case CONSTANT_INTEGER_TAG:
                case CONSTANT_FLOAT_TAG:
                case CONSTANT_NAME_AND_TYPE_TAG:
                case CONSTANT_DYNAMIC_TAG:
                case CONSTANT_INVOKE_DYNAMIC_TAG:
                    size = 5;
                    break;
                case CONSTANT_LONG_TAG:
                case CONSTANT_DOUBLE_TAG:
                    size = 9;
                    ++i;
                    break;
                case CONSTANT_UTF8_TAG:
                    final int strLen = readUnsignedShort(index + 1, basicClass);
                    size = 3 + strLen;
                    for (byte[] bytes : BYTES_TO_SEARCH) {
                        if (prefixes ? strLen >= bytes.length : strLen == bytes.length) {
                            boolean found = true;
                            for (int j = index + 3; j < index + 3 + bytes.length; j++) {
                                if (basicClass[j] != bytes[j - (index + 3)]) {
                                    found = false;
                                    break;
                                }
                            }
                            if (found) {
                                return true;
                            }
                        }
                    }
                    break;
                case CONSTANT_METHOD_HANDLE_TAG:
                    size = 4;
                    break;
                case CONSTANT_CLASS_TAG:
                case CONSTANT_STRING_TAG:
                case CONSTANT_METHOD_TYPE_TAG:
                case CONSTANT_PACKAGE_TAG:
                case CONSTANT_MODULE_TAG:
                default:
                    size = 3;
                    break;
            }
            index += size;
        }
        return false;
    }

    private static short readShort(final int index, byte[] basicClass) {
        return (short) (((basicClass[index] & 0xFF) << 8) | (basicClass[index + 1] & 0xFF));
    }

    private static int readUnsignedShort(final int index, byte[] basicClass) {
        return ((basicClass[index] & 0xFF) << 8) | (basicClass[index + 1] & 0xFF);
    }

}