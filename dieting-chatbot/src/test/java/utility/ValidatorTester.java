package utility;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@RunWith(SpringRunner.class)
public class ValidatorTester {
    @Test
    public void testIsInteger1() {
        assert Validator.isInteger("128");
        assert Validator.isInteger("-256");
        assert Validator.isInteger("+512");
    }

    @Test
    public void testIsInteger2() {
        assert !Validator.isInteger("1x28");
        assert !Validator.isInteger("notAnInteger");
        assert !Validator.isInteger("+*()#");
    }

    @Test
    public void testIsGender1() {
        assert Validator.isGender("male");
        assert Validator.isGender("female");
        assert Validator.isGender("man");
        assert Validator.isGender("woman");
    }

    @Test
    public void testIsGender2() {
        assert Validator.isGender("Male");
        assert Validator.isGender("I am female");
        assert Validator.isGender("spider mAn");
        assert Validator.isGender("woMan tester");
    }

    @Test
    public void testIsGender3() {
        assert !Validator.isGender("Not adsjfl");
        assert !Validator.isGender("ajfksdl");
        assert !Validator.isGender("293*(@)");
        assert !Validator.isGender("43905 afsd");
    }

    @Test
    public void testValidateAge() {
        assert Validator.validateAge(10);
        assert Validator.validateAge(21);
        assert !Validator.validateAge(121);
        assert !Validator.validateAge(210);
    }

    @Test
    public void testValidateWeight() {
        assert Validator.validateWeight(50);
        assert Validator.validateWeight(100);
        assert !Validator.validateWeight(1000);
        assert !Validator.validateWeight(-210);
    }

    @Test
    public void testValidateHeight() {
        assert Validator.validateHeight(150);
        assert Validator.validateHeight(200);
        assert !Validator.validateHeight(2000);
        assert !Validator.validateHeight(-290);
    }

    @Test
    public void isFutureDate() {
        assert Validator.isFutureDate("3000-1-1", "yyyy-MM-dd");
        assert Validator.isFutureDate("5000-12-31", "yyyy-MM-dd");
        assert !Validator.isFutureDate("2017-13-100", "yyyy-MM-dd");
        assert !Validator.isFutureDate("1500-3-1", "yyyy-MM-dd");
        assert !Validator.isFutureDate("1998-8-14", "yyyy-MM-dd");
        assert !Validator.isFutureDate("Not a date", "yyyy-MM-dd");
        assert !Validator.isFutureDate("123-483-234", "yyyy-MM-dd");
    }
}