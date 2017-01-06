package test.api

import com.ttpod.user.web.RegisterController
import org.junit.Test
import org.springframework.mock.web.MockHttpServletRequest
import test.BaseTest

import javax.annotation.Resource

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 13-9-9
 * Time: 下午2:52
 * To change this template use File | Settings | File Templates.
 */

class TestRegister extends BaseTest {

    @Resource
    RegisterController registerController

    /**
     * Integer OK = 1;
     Integer ERROR = 0;
     Integer 余额不足 = 30412;
     Integer 权限不足 = 30413;
     Integer 验证码验证失败 = 30419;
     Integer 用户名或密码不正确 = 30402 ;
     Integer 用户名已存在 = 30408 ;
     Integer 用户名格式错误 = 30409 ;
     Integer 密码格式错误 = 30411;
     Integer 手机号格式错误 = 30432;
     Integer 短信验证码无效 = 30431;
     Integer 手机号码已存在 = 30433;
     Integer 参数无效 = 30406;
     */
    @Test
    void testUnameRegister() {
        //1273015
        //setSession([_id: '1024418'])
        //auth_key=vz78gcht2&auth_code=2h79s&username=1234&pwd=123456
        def req = new MockHttpServletRequest()
        req.setParameter("auth_key", "x49j6nrd")
        req.setParameter("auth_code", "3jt8")
        //req.setParameter("username", "message")
        //req.setParameter("pwd", "pwd")

        def result = registerController.uname(req)
        println result
        assert null != result
        assert result["code"].equals(30406)
    }

}