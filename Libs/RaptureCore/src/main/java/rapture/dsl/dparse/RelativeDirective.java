/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2011-2016 Incapture Technologies LLC
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
package rapture.dsl.dparse;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import rapture.common.repo.CommitObject;

public class RelativeDirective extends BaseDirective {
    private List<ValueTime> values = new ArrayList<ValueTime>();

    private Calendar testDate;

    public void addValue(ValueTime valueTime) {
        values.add(valueTime);
    }

    @Override
    public boolean incorrect(CommitObject cObj) {
        Date date = cObj.getWhen();
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        if (testDate.after(c)) {
            setCommit(cObj);
            return false;
        }
        return true;
    }

    @Override
    public void reset(String context) {
        testDate = Calendar.getInstance();
        testDate.setTime(new Date());
        // Now wind it back
        for (ValueTime vt : values) {
            vt.windBack(testDate);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (ValueTime vt : values) {
            sb.append(vt.toString() + " ");
        }
        return sb.toString();
    }
}