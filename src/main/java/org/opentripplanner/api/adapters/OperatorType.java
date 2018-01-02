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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.opentripplanner.model.AgencyAndId;
import org.opentripplanner.model.Operator;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlRootElement(name = "Operator")
public class OperatorType {

    public OperatorType(Operator arg) {
        this.id = arg.getId();
        this.name = arg.getName();
        this.url = arg.getUrl();
        this.phone = arg.getPhone();
    }

    public OperatorType() {
    }

    @XmlJavaTypeAdapter(AgencyAndIdAdapter.class)
    @JsonSerialize
    AgencyAndId id;

    @XmlAttribute
    @JsonSerialize
    String name;

    @XmlAttribute
    @JsonSerialize
    String url;

    @XmlAttribute
    @JsonSerialize
    String phone;
}