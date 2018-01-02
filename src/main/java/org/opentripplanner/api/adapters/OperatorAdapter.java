/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.api.adapters;

import org.opentripplanner.model.Operator;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class OperatorAdapter extends XmlAdapter<OperatorType, Operator> {

    @Override
    public Operator unmarshal(OperatorType arg) {
        throw new UnsupportedOperationException(
                "We presently serialize Operator as OperatorType, and thus cannot deserialize them");
    }

    @Override
    public OperatorType marshal(Operator arg) {
        if (arg == null) {
            return null;
        }
        return new OperatorType(arg);
    }

}
